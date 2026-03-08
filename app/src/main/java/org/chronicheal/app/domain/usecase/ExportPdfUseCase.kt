package org.chronicheal.app.domain.usecase

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
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
import java.time.temporal.ChronoUnit
import java.util.Locale
import javax.inject.Inject

class ExportPdfUseCase @Inject constructor(
    private val repository: EntryRepository,
    @ApplicationContext private val context: Context
) {
    private val palette = listOf(
        0xFF0072B2.toInt(), // Blue
        0xFFD55E00.toInt(), // Vermillion
        0xFF009E73.toInt(), // Bluish Green
        0xFFCC79A7.toInt(), // Reddish Purple
        0xFFF0E442.toInt(), // Yellow
        0xFF56B4E9.toInt(), // Sky Blue
        0xFFE69F00.toInt(), // Orange
    )

    suspend operator fun invoke(outputStream: OutputStream, startDate: LocalDate? = null, endDate: LocalDate? = null) {
        withContext(Dispatchers.IO) {
            val allEntries = repository.getAllEntries().first()
            
            val actualStart = startDate ?: allEntries.lastOrNull()?.timestamp?.atZone(ZoneId.systemDefault())?.toLocalDate() ?: LocalDate.now()
            val actualEnd = endDate ?: LocalDate.now()

            val entries = allEntries.filter { 
                val date = it.timestamp.atZone(ZoneId.systemDefault()).toLocalDate()
                !date.isBefore(actualStart) && !date.isAfter(actualEnd)
            }

            val document = PdfDocument()
            try {
                val pageWidth = 595
                val pageHeight = 842
                val margin = 40f
                val chartWidth = pageWidth - (margin * 2)
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

                // --- 1. Pain Evolution (Stacked) ---
                val painData = getEvolutionData(entries, EntryType.PAIN, actualStart, actualEnd)
                canvas.drawText("Pain Evolution (Stacked Intensity)", margin, y, headerPaint)
                y += 15f
                canvas.drawText("This graph shows the cumulative intensity of pain across different locations over time.", margin, y, textPaint)
                y += 20f
                y = drawEvolutionChart(canvas, painData, margin, y, chartWidth, axisPaint, textPaint)
                y += 40f

                // --- 2. Symptoms Evolution (Stacked) ---
                val symptomData = getEvolutionData(entries, EntryType.SYMPTOM, actualStart, actualEnd)
                if (y > pageHeight - 200f) {
                    document.finishPage(page)
                    page = document.startPage(pageInfo)
                    canvas = page.canvas
                    canvas.drawColor(Color.WHITE)
                    y = margin + 20f
                }
                canvas.drawText("Symptoms Evolution (Stacked Intensity)", margin, y, headerPaint)
                y += 15f
                canvas.drawText("This graph visualizes the daily impact of various symptoms, stacked to show overall burden.", margin, y, textPaint)
                y += 20f
                y = drawEvolutionChart(canvas, symptomData, margin, y, chartWidth, axisPaint, textPaint)
                y += 40f

                // --- 3. Summary Table ---
                if (y > pageHeight - 200f) {
                    document.finishPage(page)
                    page = document.startPage(pageInfo)
                    canvas = page.canvas
                    canvas.drawColor(Color.WHITE)
                    y = margin + 20f
                }
                y = drawSummaryTable(canvas, entries, margin, y, chartWidth, headerPaint, textPaint)
                y += 40f

                // --- 4. Detailed Logs ---
                if (y > pageHeight - 100f) {
                    document.finishPage(page)
                    page = document.startPage(pageInfo)
                    canvas = page.canvas
                    canvas.drawColor(Color.WHITE)
                    y = margin + 20f
                }
                canvas.drawText("Detailed Logs", margin, y, headerPaint)
                y += 20f

                val dateFormater = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault())

                entries.forEach { entry ->
                    if (y > pageHeight - margin) {
                        document.finishPage(page)
                        page = document.startPage(pageInfo)
                        canvas = page.canvas
                        canvas.drawColor(Color.WHITE)
                        y = margin + 20f
                    }

                    val dateStr = dateFormater.format(entry.timestamp)
                    val typeStr = entry.type.name.lowercase().replaceFirstChar { it.uppercase() }
                    val nameStr = if (entry.name != null) "(${entry.name}) " else ""
                    val valueStr = if (entry.intensity != null) "Level: ${entry.intensity} " else ""
                    val noteStr = if (entry.note.isNotBlank()) "- ${entry.note}" else ""
                    
                    val fullLine = "$dateStr - $typeStr: $nameStr$valueStr$noteStr"
                    
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
                                y = margin + 20f
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

    private fun getEvolutionData(
        entries: List<HealthEntry>,
        type: EntryType,
        start: LocalDate,
        end: LocalDate
    ): Map<String, Map<LocalDate, Int>> {
        val filteredEntries = entries.filter { it.type == type && it.intensity != null }
        val keys = filteredEntries.mapNotNull { it.name ?: it.location }.distinct()
        
        return keys.associateWith { key ->
            val keyEntries = filteredEntries.filter { (it.name ?: it.location) == key }
            val daysBetween = ChronoUnit.DAYS.between(start, end).toInt()
            (0..daysBetween).associate { i ->
                val date = start.plusDays(i.toLong())
                val dayEntries = keyEntries.filter { it.timestamp.atZone(ZoneId.systemDefault()).toLocalDate() == date }
                date to if (dayEntries.isEmpty()) 0 else dayEntries.mapNotNull { it.intensity }.average().toInt()
            }
        }
    }

    private fun drawEvolutionChart(
        canvas: Canvas,
        data: Map<String, Map<LocalDate, Int>>,
        margin: Float,
        startY: Float,
        chartWidth: Float,
        axisPaint: Paint,
        textPaint: Paint
    ): Float {
        var y = startY
        val chartHeight = 120f
        if (data.isEmpty()) {
            canvas.drawText("No data available for this period.", margin, y + 20f, textPaint)
            return y + 40f
        }

        val allKeys = data.keys.toList()
        val allDates = data.values.first().keys.sorted()
        val daysBetween = allDates.size - 1
        val stepX = chartWidth / daysBetween.coerceAtLeast(1)

        // Calculate stacked values
        val stackedValues = mutableListOf<Map<LocalDate, Float>>()
        allKeys.forEachIndexed { i, key ->
            val currentRaw = data[key]!!
            val currentStacked = allDates.associateWith { date ->
                val previousValue = if (i > 0) stackedValues[i - 1][date] ?: 0f else 0f
                previousValue + (currentRaw[date] ?: 0).toFloat()
            }
            stackedValues.add(currentStacked)
        }

        val maxTotal = (stackedValues.lastOrNull()?.values?.maxOrNull() ?: 10f).coerceAtLeast(1f)

        // Grid and Ticks
        val gridPaint = Paint(axisPaint).apply { 
            color = Color.LTGRAY
            alpha = 100
            strokeWidth = 0.5f 
        }
        val tickPaint = Paint(axisPaint)
        
        // Horizontal Grid and Y-Ticks
        val horizontalSteps = 5
        for (i in 0..horizontalSteps) {
            val gridY = y + chartHeight - (i * (chartHeight / horizontalSteps))
            canvas.drawLine(margin, gridY, margin + chartWidth, gridY, gridPaint)
            canvas.drawLine(margin - 5f, gridY, margin, gridY, tickPaint)
            val label = (i * (maxTotal / horizontalSteps)).toInt().toString()
            canvas.drawText(label, margin - 25f, gridY + 4f, textPaint)
        }

        // Vertical Grid and X-Ticks (Date markers)
        val verticalSteps = if (daysBetween < 10) daysBetween else 7
        for (i in 0..verticalSteps) {
            val dayIndex = i * (daysBetween / verticalSteps)
            val gridX = margin + (dayIndex * stepX)
            canvas.drawLine(gridX, y, gridX, y + chartHeight, gridPaint)
            canvas.drawLine(gridX, y + chartHeight, gridX, y + chartHeight + 5f, tickPaint)
            
            val dateLabel = allDates.getOrNull(dayIndex)?.format(DateTimeFormatter.ofPattern("MM/dd")) ?: ""
            canvas.drawText(dateLabel, gridX - 10f, y + chartHeight + 15f, textPaint)
        }

        // Draw stacked areas
        for (i in allKeys.indices.reversed()) {
            val color = palette[i % palette.size]
            val areaPaint = Paint().apply {
                this.color = color
                style = Paint.Style.FILL
                alpha = 180
            }
            val linePaint = Paint().apply {
                this.color = color
                style = Paint.Style.STROKE
                strokeWidth = 1.5f
                isAntiAlias = true
            }

            val path = Path()
            path.moveTo(margin, y + chartHeight)
            
            allDates.forEachIndexed { index, date ->
                val valY = stackedValues[i][date] ?: 0f
                val px = margin + (index * stepX)
                val py = y + chartHeight - (valY * (chartHeight / maxTotal))
                path.lineTo(px, py)
            }
            
            path.lineTo(margin + chartWidth, y + chartHeight)
            path.close()
            canvas.drawPath(path, areaPaint)
            
            val linePath = Path()
            allDates.forEachIndexed { index, date ->
                val valY = stackedValues[i][date] ?: 0f
                val px = margin + (index * stepX)
                val py = y + chartHeight - (valY * (chartHeight / maxTotal))
                if (index == 0) linePath.moveTo(px, py) else linePath.lineTo(px, py)
            }
            canvas.drawPath(linePath, linePaint)
        }

        // Axes
        canvas.drawLine(margin, y, margin, y + chartHeight, tickPaint)
        canvas.drawLine(margin, y + chartHeight, margin + chartWidth, y + chartHeight, tickPaint)

        y += chartHeight + 35f
        
        // Legend
        var currentX = margin
        allKeys.forEachIndexed { index, key ->
            val color = palette[index % palette.size]
            val legPaint = Paint().apply { this.color = color; style = Paint.Style.FILL }
            
            if (currentX + textPaint.measureText(key) + 20f > margin + chartWidth) {
                currentX = margin
                y += 15f
            }
            
            canvas.drawRect(currentX, y - 8f, currentX + 10f, y, legPaint)
            canvas.drawText(key, currentX + 15f, y, textPaint)
            currentX += textPaint.measureText(key) + 30f
        }

        return y + 20f
    }

    private fun drawSummaryTable(
        canvas: Canvas,
        entries: List<HealthEntry>,
        margin: Float,
        startY: Float,
        tableWidth: Float,
        headerPaint: Paint,
        textPaint: Paint
    ): Float {
        var y = startY
        canvas.drawText("Period Summary Statistics", margin, y, headerPaint)
        y += 25f

        val metrics = entries.filter { (it.type == EntryType.PAIN || it.type == EntryType.SYMPTOM) && it.intensity != null }
            .groupBy { it.name ?: it.location ?: "General" }
            .mapValues { (_, group) ->
                val intensities = group.mapNotNull { it.intensity }
                Triple(intensities.minOrNull() ?: 0, intensities.maxOrNull() ?: 0, intensities.average())
            }

        if (metrics.isEmpty()) {
            canvas.drawText("No pain or symptom data for statistics.", margin, y, textPaint)
            return y + 20f
        }

        // Table Header
        val col1 = margin
        val col2 = margin + (tableWidth * 0.4f)
        val col3 = margin + (tableWidth * 0.6f)
        val col4 = margin + (tableWidth * 0.8f)

        val tableHeaderPaint = Paint(textPaint).apply { isFakeBoldText = true }
        canvas.drawText("Metric", col1, y, tableHeaderPaint)
        canvas.drawText("Min", col2, y, tableHeaderPaint)
        canvas.drawText("Max", col3, y, tableHeaderPaint)
        canvas.drawText("Avg", col4, y, tableHeaderPaint)
        y += 5f
        canvas.drawLine(margin, y, margin + tableWidth, y, Paint().apply { color = Color.BLACK; strokeWidth = 1f })
        y += 15f

        metrics.forEach { (name, stats) ->
            canvas.drawText(name, col1, y, textPaint)
            canvas.drawText(stats.first.toString(), col2, y, textPaint)
            canvas.drawText(stats.second.toString(), col3, y, textPaint)
            canvas.drawText(String.format(Locale.getDefault(), "%.1f", stats.third), col4, y, textPaint)
            y += 15f
            
            canvas.drawLine(margin, y - 2f, margin + tableWidth, y - 2f, Paint().apply { color = Color.LTGRAY; strokeWidth = 0.5f })
            y += 5f
        }

        return y
    }
}
