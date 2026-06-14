package org.chronicheal.app

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
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
}
