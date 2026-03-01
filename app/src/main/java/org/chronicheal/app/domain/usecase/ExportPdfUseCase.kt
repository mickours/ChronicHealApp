package org.chronicheal.app.domain.usecase

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import org.chronicheal.app.domain.repository.EntryRepository
import java.io.OutputStream
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

class ExportPdfUseCase @Inject constructor(
    private val repository: EntryRepository,
    @ApplicationContext private val context: Context
) {
    suspend operator fun invoke(outputStream: OutputStream) {
        val entries = repository.getAllEntries().first()
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 size
        var page = document.startPage(pageInfo)
        var canvas = page.canvas
        val paint = Paint()
        val titlePaint = Paint().apply {
            textSize = 18f
            isFakeBoldText = true
        }
        val textPaint = Paint().apply {
            textSize = 12f
        }
        val dateFormater = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault())

        var y = 40f
        canvas.drawText("ChronicHeal Health Report", 40f, y, titlePaint)
        y += 30f

        entries.forEach { entry ->
            if (y > 800f) {
                document.finishPage(page)
                page = document.startPage(pageInfo)
                canvas = page.canvas
                y = 40f
            }

            val dateStr = dateFormater.format(entry.timestamp)
            canvas.drawText("$dateStr - ${entry.type}: ${entry.name ?: ""} ${entry.note}", 40f, y, textPaint)
            y += 20f
        }

        document.finishPage(page)
        document.writeTo(outputStream)
        document.close()
    }
}
