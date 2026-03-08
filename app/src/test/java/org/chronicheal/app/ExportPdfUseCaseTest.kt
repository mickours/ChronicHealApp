package org.chronicheal.app

import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.chronicheal.app.domain.model.EntryType
import org.chronicheal.app.domain.model.HealthEntry
import org.chronicheal.app.domain.repository.EntryRepository
import org.chronicheal.app.domain.usecase.ExportPdfUseCase
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.time.Instant
import java.time.LocalDate

class ExportPdfUseCaseTest {

    private lateinit var repository: EntryRepository
    private lateinit var exportPdfUseCase: ExportPdfUseCase

    @Before
    fun setup() {
        repository = mockk()
        exportPdfUseCase = ExportPdfUseCase(repository)
    }

    @Test
    fun `invoke should write non-empty PDF content to output stream`() = runBlocking {
        // Arrange
        val entries = listOf(
            HealthEntry(
                id = 1,
                timestamp = Instant.now(),
                type = EntryType.PAIN,
                intensity = 5,
                location = "Back"
            ),
            HealthEntry(
                id = 2,
                timestamp = Instant.now(),
                type = EntryType.SYMPTOM,
                name = "Headache",
                intensity = 3
            )
        )
        every { repository.getAllEntries() } returns flowOf(entries)

        val outputStream = ByteArrayOutputStream()

        // Act
        exportPdfUseCase(outputStream, LocalDate.now().minusDays(7), LocalDate.now())

        // Assert
        val pdfBytes = outputStream.toByteArray()
        assertTrue("PDF content should not be empty", pdfBytes.isNotEmpty())
        
        // PDF files start with %PDF
        val pdfString = String(pdfBytes.take(10).toByteArray())
        assertTrue("Output should be a PDF file", pdfString.startsWith("%PDF"))
    }
}
