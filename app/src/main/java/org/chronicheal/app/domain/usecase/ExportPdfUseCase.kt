package org.chronicheal.app.domain.usecase

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.chronicheal.app.domain.model.EntryType
import org.chronicheal.app.domain.model.HealthEntry
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
            
            val actualStart = startDate ?: entries.lastOrNull()?.timestamp?.atZone(ZoneId.systemDefault())?.toLocalDate() ?: LocalDate.now()
            val actualEnd = endDate ?: LocalDate.now()

            entries = entries.filter { 
                val date = it.timestamp.atZone(ZoneId.systemDefault()).toLocalDate()
                !date.isBefore(actualStart) && !date.isAfter(actualEnd)
            }

            val document = PdfDocument()
            try {
                val pageWidth = 595
                val pageHeight = 842
                val margin = 40f
                val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
                var page = document.startPage(pageInfo)
                var canvas = page.canvas
                
                canvas.drawColor(Color.WHITE)

                val titlePaint = Paint().apply {
                    color = Color.BLACK
                    textSize = 20f
                    isFakeBoldText = true
                }
                val headerPaint = Paint().apply {
                    color = Color.BLACK
                    textSize = 14f
                    isFakeBoldText = true
                }
                val textPaint = Paint().apply {
                    color = Color.BLACK
                    textSize = 10f
                }
                val chartLinePaint = Paint().apply {
                    color = 0xFF0072B2.toInt() // Blue
                    strokeWidth = 2f
                    style = Paint.Style.STROKE
                    isAntiAlias = true
                }
                val chartBarPaint = Paint().apply {
                    color = 0xFFD55E00.toInt() // Vermillion
                    style = Paint.Style.FILL
                }
                val axisPaint = Paint().apply {
                    color = Color.LTGRAY
                    strokeWidth = 1f
                }

                var y = margin + 20f
                canvas.drawText("ChronicHeal Health Report", margin, y, titlePaint)
                y += 25f
                
                val rangeStr = "Period: $actualStart to $actualEnd"
                canvas.drawText(rangeStr, margin, y, textPaint)
                y += 40f

                // --- 1. Pain Evolution Graph ---
                canvas.drawText("Pain Evolution (0-10)", margin, y, headerPaint)
                y += 20f
                
                val chartHeight = 100f
                val chartWidth = pageWidth - (margin * 2)
                
                // Draw Axes
                canvas.drawLine(margin, y, margin + chartWidth, y, axisPaint) // Top
                canvas.drawLine(margin, y + chartHeight, margin + chartWidth, y + chartHeight, axisPaint) // Bottom
                
                val painData = entries
                    .filter { it.type == EntryType.PAIN && it.intensity != null }
                    .groupBy { it.timestamp.atZone(ZoneId.systemDefault()).toLocalDate() }
                    .mapValues { it.value.mapNotNull { e -> e.intensity }.average().toFloat() }

                val daysBetween = java.time.temporal.ChronoUnit.DAYS.between(actualStart, actualEnd).coerceAtLeast(1)
                val stepX = chartWidth / daysBetween
                
                var lastX = -1f
                var lastY = -1f
                
                for (i in 0..daysBetween) {
                    val date = actualStart.plusDays(i)
                    val intensity = painData[date] ?: 0f
                    val currentX = margin + (i * stepX)
                    val currentY = y + chartHeight - (intensity * (chartHeight / 10f))
                    
                    if (lastX != -1f) {
                        canvas.drawLine(lastX, lastY, currentX, currentY, chartLinePaint)
                    }
                    canvas.drawCircle(currentX, currentY, 2f, chartLinePaint.apply { style = Paint.Style.FILL })
                    chartLinePaint.apply { style = Paint.Style.STROKE }
                    
                    lastX = currentX
                    lastY = currentY
                }
                y += chartHeight + 40f

                // --- 2. Top Symptoms ---
                canvas.drawText("Top Symptoms Frequency", margin, y, headerPaint)
                y += 20f
                
                val symptomFreq = entries
                    .filter { it.type == EntryType.SYMPTOM && it.name != null }
                    .groupBy { it.name!! }
                    .mapValues { it.value.size }
                    .toList()
                    .sortedByDescending { it.second }
                    .take(5)

                if (symptomFreq.isNotEmpty()) {
                    val maxFreq = symptomFreq.maxOf { it.second }.toFloat().coerceAtLeast(1f)
                    val barHeight = 15f
                    val barSpacing = 5f
                    
                    symptomFreq.forEach { (name, count) ->
                        val barWidth = (count / maxFreq) * chartWidth
                        canvas.drawRect(margin, y, margin + barWidth, y + barHeight, chartBarPaint)
                        canvas.drawText("$name ($count)", margin + barWidth + 5f, y + 12f, textPaint)
                        y += barHeight + barSpacing
                    }
                } else {
                    canvas.drawText("No symptom data recorded.", margin, y, textPaint)
                    y += 20f
                }
                
                y += 30f
                canvas.drawText("Detailed Logs", margin, y, headerPaint)
                y += 20f

                val dateFormater = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault())

                entries.forEach { entry ->
                    if (y > pageHeight - margin) {
                        document.finishPage(page)
                        page = document.startPage(pageInfo)
                        canvas = page.canvas
                        canvas.drawColor(Color.WHITE)
                        y = margin
                    }

                    val dateStr = dateFormater.format(entry.timestamp)
                    val typeStr = entry.type.name.lowercase().replaceFirstChar { it.uppercase() }
                    val nameStr = if (entry.name != null) "(${entry.name}) " else ""
                    val valueStr = if (entry.intensity != null) "Level: ${entry.intensity} " else ""
                    val noteStr = if (entry.note.isNotBlank()) "- ${entry.note}" else ""
                    
                    val fullLine = "$dateStr - $typeStr: $nameStr$valueStr$noteStr"
                    
                    // Simple multiline support
                    val words = fullLine.split(" ")
                    var line = ""
                    words.forEach { word ->
                        if (textPaint.measureText(line + word) < chartWidth) {
                            line += "$word "
                        } else {
                            canvas.drawText(line, margin, y, textPaint)
                            y += 15f
                            line = "  $word "
                            if (y > pageHeight - margin) {
                                document.finishPage(page)
                                page = document.startPage(pageInfo)
                                canvas = page.canvas
                                canvas.drawColor(Color.WHITE)
                                y = margin
                            }
                        }
                    }
                    canvas.drawText(line, margin, y, textPaint)
                    y += 18f
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
