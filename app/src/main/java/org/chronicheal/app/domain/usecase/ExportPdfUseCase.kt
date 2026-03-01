package org.chronicheal.app.domain.usecase

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.chronicheal.app.domain.repository.EntryRepository
import java.io.OutputStream
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

class ExportPdfUseCase @Inject constructor(
    private val repository: EntryRepository,
    @ApplicationContext private val context: Context
) {
    suspend operator fun invoke(outputStream: OutputStream, startDate: LocalDate? = null, endDate: LocalDate? = null) {
        withContext(Dispatchers.IO) {
            var entries = repository.getAllEntries().first()
            
            if (startDate != null) {
                entries = entries.filter { 
                    val date = it.timestamp.atZone(ZoneId.systemDefault()).toLocalDate()
                    !date.isBefore(startDate) 
                }
            }
            if (endDate != null) {
                entries = entries.filter { 
                    val date = it.timestamp.atZone(ZoneId.systemDefault()).toLocalDate()
                    !date.isAfter(endDate) 
                }
            }

            val document = PdfDocument()
            try {
                val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 size
                var page = document.startPage(pageInfo)
                var canvas = page.canvas
                
                // Set background to white
                canvas.drawColor(Color.WHITE)

                val titlePaint = Paint().apply {
                    color = Color.BLACK
                    textSize = 18f
                    isFakeBoldText = true
                }
                val textPaint = Paint().apply {
                    color = Color.BLACK
                    textSize = 12f
                }
                val dateFormater = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault())

                var y = 40f
                canvas.drawText("ChronicHeal Health Report", 40f, y, titlePaint)
                y += 25f
                
                val rangeStr = when {
                    startDate != null && endDate != null -> "Range: $startDate to $endDate"
                    startDate != null -> "From: $startDate"
                    endDate != null -> "Until: $endDate"
                    else -> "Full History"
                }
                canvas.drawText(rangeStr, 40f, y, textPaint)
                y += 35f

                entries.forEach { entry ->
                    if (y > 800f) {
                        document.finishPage(page)
                        page = document.startPage(pageInfo)
                        canvas = page.canvas
                        canvas.drawColor(Color.WHITE)
                        y = 40f
                    }

                    val dateStr = dateFormater.format(entry.timestamp)
                    val typeStr = entry.type.name.lowercase().replaceFirstChar { it.uppercase() }
                    val nameStr = if (entry.name != null) "(${entry.name}) " else ""
                    val noteStr = if (entry.note.isNotBlank()) "- ${entry.note}" else ""
                    
                    val fullLine = "$dateStr - $typeStr: $nameStr$noteStr"
                    // Simple text wrapping if line is too long
                    if (fullLine.length > 80) {
                        canvas.drawText(fullLine.substring(0, 80) + "...", 40f, y, textPaint)
                    } else {
                        canvas.drawText(fullLine, 40f, y, textPaint)
                    }
                    y += 20f
                }

                document.finishPage(page)
                document.writeTo(outputStream)
                outputStream.flush()
            } finally {
                document.close()
            }
        }
    }
}
