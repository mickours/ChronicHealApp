package org.chronicheal.app

import android.util.Log
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.chronicheal.app.data.local.AppDatabase
import org.chronicheal.app.domain.model.EntryType
import org.chronicheal.app.domain.model.HealthEntry
import org.chronicheal.app.domain.repository.HealthRepository
import org.chronicheal.app.domain.usecase.ExportDataUseCase
import org.chronicheal.app.domain.usecase.ImportDataUseCase
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.Instant

class DataTransferTest {

    private lateinit var repository: HealthRepository
    private lateinit var database: AppDatabase
    private lateinit var exportDataUseCase: ExportDataUseCase
    private lateinit var importDataUseCase: ImportDataUseCase

    @Before
    fun setup() {
        mockkStatic(Log::class)
        every { Log.e(any(), any(), any()) } returns 0
        every { Log.w(any(), any<String>()) } returns 0

        repository = mockk()
        database = mockk(relaxed = true)
        every { database.openHelper.readableDatabase.version } returns 1
        exportDataUseCase = ExportDataUseCase(repository, database)
        importDataUseCase = ImportDataUseCase(repository, database)
    }

    @Test
    fun `export and import should preserve all data`() = runBlocking {
        // 1. Arrange: Create sample data
        val originalEntries = listOf(
            HealthEntry(
                id = 1,
                timestamp = Instant.parse("2023-10-27T10:00:00Z"),
                type = EntryType.PAIN,
                intensity = 7,
                location = "Lower Back",
                note = "Sharp pain after lifting"
            ),
            HealthEntry(
                id = 2,
                timestamp = Instant.parse("2023-10-27T12:00:00Z"),
                type = EntryType.DRUG,
                name = "Ibuprofen",
                value = 400.0,
                unit = "mg",
                note = "With food"
            ),
            HealthEntry(
                id = 3,
                timestamp = Instant.parse("2023-10-27T22:00:00Z"),
                type = EntryType.SLEEP,
                durationMinutes = 480,
                intensity = 4 // Quality
            )
        )

        // Mock repository to return the original entries
        every { repository.getAllEntries() } returns flowOf(originalEntries)
        
        // Mock repository insert to capture the imported entries
        val importedEntriesSlot = mutableListOf<List<HealthEntry>>()
        coEvery { repository.insertEntries(capture(importedEntriesSlot)) } returns Unit

        // 2. Act: Export to JSON and then Import back
        val jsonOutput = exportDataUseCase()
        importDataUseCase(jsonOutput)

        // 3. Assert: Compare original with imported
        val importedEntries = importedEntriesSlot.first()
        
        assertEquals("Number of entries should match", originalEntries.size, importedEntries.size)
        
        originalEntries.forEachIndexed { index, original ->
            val imported = importedEntries[index]
            assertEquals("ID should match for entry $index", original.id, imported.id)
            assertEquals("Timestamp should match for entry $index", original.timestamp, imported.timestamp)
            assertEquals("Type should match for entry $index", original.type, imported.type)
            assertEquals("Note should match for entry $index", original.note, imported.note)
            assertEquals("Intensity should match for entry $index", original.intensity, imported.intensity)
            assertEquals("Location should match for entry $index", original.location, imported.location)
            assertEquals("Name should match for entry $index", original.name, imported.name)
            assertEquals("Value should match for entry $index", original.value, imported.value)
            assertEquals("Unit should match for entry $index", original.unit, imported.unit)
            assertEquals("Duration should match for entry $index", original.durationMinutes, imported.durationMinutes)
        }
        
        coVerify(exactly = 1) { repository.insertEntries(any()) }
    }

    @Test
    fun `import should handle old format (list of entries)`() = runBlocking {
        // Arrange
        val oldJson = """
            [
                {
                    "id": 10,
                    "timestamp": 1698400800000,
                    "type": "PAIN",
                    "intensity": 5
                }
            ]
        """.trimIndent()

        val entriesSlot = mutableListOf<List<HealthEntry>>()
        coEvery { repository.insertEntries(capture(entriesSlot)) } returns Unit

        // Act
        importDataUseCase(oldJson)

        // Assert
        assertEquals(1, entriesSlot.first().size)
        assertEquals(10L, entriesSlot.first()[0].id)
        assertEquals(EntryType.PAIN, entriesSlot.first()[0].type)
    }

    @Test(expected = ImportDataUseCase.VersionMismatchException::class)
    fun `import should throw VersionMismatchException when backup version is newer`() =
        runBlocking {
            // Arrange
            val newerJson = """
            {
                "schemaVersion": 99,
                "entries": [
                    {
                        "id": 1,
                        "timestamp": 1698400800000,
                        "type": "PAIN"
                    }
                ]
            }
        """.trimIndent()

            every { database.openHelper.readableDatabase.version } returns 1

            // Act
            importDataUseCase(newerJson)
        }

    @Test(expected = IllegalArgumentException::class)
    fun `import should throw IllegalArgumentException for invalid json`() = runBlocking {
        // Arrange
        val invalidJson = "not a json"

        // Act
        importDataUseCase(invalidJson)
    }

    @Test
    fun `import actual backup file should succeed`() = runBlocking {
        // Arrange
        val backupJson = java.io.File("../chronicheal_backup.json").readText()
        val entriesSlot = mutableListOf<List<HealthEntry>>()
        coEvery { repository.insertEntries(capture(entriesSlot)) } returns Unit
        every { database.openHelper.readableDatabase.version } returns 11

        // Act
        importDataUseCase(backupJson)

        // Assert
        coVerify(exactly = 1) { repository.insertEntries(any()) }
        val imported = entriesSlot.first()
        assertEquals(539, imported.size)
    }
}
