package org.chronicheal.app.domain.usecase

import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.chronicheal.app.domain.model.EntryType
import org.chronicheal.app.domain.model.HealthEntry
import org.chronicheal.app.domain.repository.HealthRepository
import java.io.OutputStream
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import javax.inject.Inject

class ExportPdfUseCase @Inject constructor(
    private val repository: HealthRepository
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

    private class PageState(
        val document: PdfDocument,
        val pageWidth: Int,
        val pageHeight: Int,
        val margin: Float
    ) {
        var pageNum = 1
        var pageInfo: PdfDocument.PageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create()
        var page: PdfDocument.Page = document.startPage(pageInfo)
        var canvas: android.graphics.Canvas = page.canvas
        var y = margin + 20f

        fun checkNewPage(neededHeight: Float) {
            if (y + neededHeight > pageHeight - margin) {
                document.finishPage(page)
                pageNum++
                pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create()
                page = document.startPage(pageInfo)
                canvas = page.canvas
                canvas.drawColor(Color.WHITE)
                y = margin + 20f
            }
        }

        fun drawText(text: String, x: Float, paint: Paint) {
            canvas.drawText(text, x, y, paint)
        }

        fun advance(height: Float) {
            y += height
        }
    }

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
                val state = PageState(document, 595, 842, 40f)
                state.canvas.drawColor(Color.WHITE)

                val titlePaint = Paint().apply { color = Color.BLACK; textSize = 18f; isFakeBoldText = true }
                val headerPaint = Paint().apply { color = Color.BLACK; textSize = 12f; isFakeBoldText = true }
                val textPaint = Paint().apply { color = Color.BLACK; textSize = 9f }
                val axisPaint = Paint().apply { color = Color.LTGRAY; strokeWidth = 0.8f }

                // Title
                state.drawText("ChronicHeal Health Report", state.margin, titlePaint)
                state.advance(20f)
                state.drawText("Period: $actualStart to $actualEnd", state.margin, textPaint)
                state.advance(30f)

                // --- 1. Pain Evolution ---
                val painData = getEvolutionData(entries, EntryType.PAIN, actualStart, actualEnd)
                state.checkNewPage(180f)
                state.drawText("Pain Evolution (Stacked Intensity)", state.margin, headerPaint)
                state.advance(15f)
                state.drawText("Cumulative intensity of pain across different locations over time.", state.margin, textPaint)
                state.advance(15f)
                drawEvolutionChart(state, painData, axisPaint, textPaint)
                state.advance(45f)

                // --- 2. Symptoms Evolution ---
                val symptomData = getEvolutionData(entries, EntryType.SYMPTOM, actualStart, actualEnd)
                state.checkNewPage(180f)
                state.drawText("Symptoms Evolution (Stacked Intensity)", state.margin, headerPaint)
                state.advance(15f)
                state.drawText("Daily impact of various symptoms, stacked to show overall burden.", state.margin, textPaint)
                state.advance(15f)
                drawEvolutionChart(state, symptomData, axisPaint, textPaint)
                state.advance(45f)

                // --- 3. Intensity Distribution ---
                state.checkNewPage(200f)
                drawDistributionHistograms(state, entries, headerPaint, textPaint)
                state.advance(35f)

                // --- 4. Detailed Logs (Optimized) ---
                state.checkNewPage(50f)
                state.drawText("Detailed Logs", state.margin, headerPaint)
                state.advance(15f)

                val logDateFormatter = DateTimeFormatter.ofPattern("HH:mm")
                val dayGroupFormatter = DateTimeFormatter.ofPattern("EEEE, MMM dd, yyyy")
                
                val entriesByDay = entries.groupBy { it.timestamp.atZone(ZoneId.systemDefault()).toLocalDate() }
                    .toSortedMap(compareByDescending { it })

                entriesByDay.forEach { (date, dayEntries) ->
                    state.checkNewPage(20f)
                    val dayHeaderPaint = Paint(textPaint).apply { isFakeBoldText = true; textSize = 10f }
                    state.drawText(date.format(dayGroupFormatter), state.margin, dayHeaderPaint)
                    state.advance(14f)

                    dayEntries.sortedByDescending { it.timestamp }.forEach { entry ->
                        state.checkNewPage(12f)
                        
                        val timeStr = entry.timestamp.atZone(ZoneId.systemDefault()).format(logDateFormatter)
                        val typeEmoji = entry.type?.emoji
                        
                        val content = buildString {
                            append("$timeStr $typeEmoji ")
                            if (entry.type == EntryType.PAIN && entry.location != null) append("[${entry.location}] ")
                            if (entry.name != null) append("${entry.name} ")
                            if (entry.intensity != null) append("Int: ${entry.intensity}/10 ")
                            if (entry.value != null) append("${entry.value}${entry.unit ?: ""} ")
                            if (entry.note.isNotBlank()) append("| ${entry.note.take(100)}") // Cap notes length
                        }

                        state.drawText(content, state.margin + 10f, textPaint)
                        state.advance(12f)
                    }
                    state.advance(5f)
                }

                document.finishPage(state.page)
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
        state: PageState,
        data: Map<String, Map<LocalDate, Int>>,
        axisPaint: Paint,
        textPaint: Paint
    ) {
        val chartHeight = 100f
        val chartWidth = state.pageWidth - (state.margin * 2)
        
        if (data.isEmpty()) {
            state.drawText("No data available for this period.", state.margin, textPaint)
            state.advance(20f)
            return
        }

        val allKeys = data.keys.toList()
        val allDates = data.values.first().keys.sorted()
        val daysBetween = allDates.size - 1
        val stepX = chartWidth / daysBetween.coerceAtLeast(1)

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

        // Horizontal Grid
        val gridPaint = Paint(axisPaint).apply { alpha = 60; strokeWidth = 0.5f }
        for (i in 0..4) {
            val gridY = state.y + chartHeight - (i * (chartHeight / 4))
            state.canvas.drawLine(state.margin, gridY, state.margin + chartWidth, gridY, gridPaint)
            state.canvas.drawText((i * (maxTotal / 4)).toInt().toString(), state.margin - 18f, gridY + 3f, textPaint)
        }

        // X-axis and Ticks
        state.canvas.drawLine(state.margin, state.y + chartHeight, state.margin + chartWidth, state.y + chartHeight, axisPaint)
        
        val maxLabels = 7
        val labelStep = (allDates.size / maxLabels).coerceAtLeast(1)
        val dateFormatter = DateTimeFormatter.ofPattern("MMM dd", Locale.getDefault())
        
        allDates.forEachIndexed { index, date ->
            if (index % labelStep == 0 || index == allDates.size - 1) {
                val tickX = state.margin + (index * stepX)
                state.canvas.drawLine(tickX, state.y + chartHeight, tickX, state.y + chartHeight + 4f, axisPaint)
                
                val label = date.format(dateFormatter)
                val labelWidth = textPaint.measureText(label)
                state.canvas.drawText(label, tickX - (labelWidth / 2), state.y + chartHeight + 14f, textPaint)
            }
        }

        // Draw Areas
        for (i in allKeys.indices.reversed()) {
            val color = palette[i % palette.size]
            val areaPaint = Paint().apply { this.color = color; style = Paint.Style.FILL; alpha = 140 }
            val path = Path()
            path.moveTo(state.margin, state.y + chartHeight)
            allDates.forEachIndexed { index, date ->
                val valY = stackedValues[i][date] ?: 0f
                path.lineTo(state.margin + (index * stepX), state.y + chartHeight - (valY * (chartHeight / maxTotal)))
            }
            path.lineTo(state.margin + chartWidth, state.y + chartHeight)
            path.close()
            state.canvas.drawPath(path, areaPaint)
        }

        state.advance(chartHeight + 25f)
        
        // Compact Legend
        var curX = state.margin
        allKeys.forEachIndexed { index, key ->
            val color = palette[index % palette.size]
            val p = Paint().apply { this.color = color; style = Paint.Style.FILL }
            if (curX + textPaint.measureText(key) + 20f > state.margin + chartWidth) {
                curX = state.margin
                state.advance(12f)
            }
            state.canvas.drawRect(curX, state.y - 7f, curX + 8f, state.y, p)
            state.canvas.drawText(key, curX + 12f, state.y, textPaint)
            curX += textPaint.measureText(key) + 25f
        }
        state.advance(10f)
    }

    private fun drawDistributionHistograms(
        state: PageState,
        entries: List<HealthEntry>,
        headerPaint: Paint,
        textPaint: Paint
    ) {
        state.drawText("Intensity Distribution (1-10)", state.margin, headerPaint)
        state.advance(15f)
        state.drawText("Frequency of each intensity level for recorded metrics.", state.margin, textPaint)
        state.advance(20f)

        val metrics = entries.filter { (it.type == EntryType.PAIN || it.type == EntryType.SYMPTOM) && it.intensity != null }
            .groupBy { it.name ?: it.location ?: "General" }
            .toList()
            .sortedBy { it.first }

        if (metrics.isEmpty()) {
            state.drawText("No data for distribution.", state.margin, textPaint)
            state.advance(12f)
            return
        }

        val histoHeight = 40f
        val histoWidth = 140f
        val spacing = 20f
        var curX = state.margin
        val axisPaint = Paint().apply { color = Color.LTGRAY; strokeWidth = 0.5f }

        metrics.forEachIndexed { index, (name, group) ->
            state.checkNewPage(histoHeight + 40f)
            
            // Layout: 3 histograms per row
            if (curX + histoWidth > state.pageWidth - state.margin) {
                curX = state.margin
                state.advance(histoHeight + 50f)
                state.checkNewPage(histoHeight + 40f)
            }

            // Calculate Distribution
            val counts = (1..10).associateWith { level -> group.count { it.intensity == level } }
            val maxCount = (counts.values.maxOrNull() ?: 1).coerceAtLeast(1)

            // Draw Metric Name
            val namePaint = Paint(textPaint).apply { isFakeBoldText = true }
            state.canvas.drawText(name.take(25), curX, state.y - 5f, namePaint)

            // Draw Axes
            state.canvas.drawLine(curX, state.y, curX, state.y + histoHeight, axisPaint)
            state.canvas.drawLine(curX, state.y + histoHeight, curX + histoWidth, state.y + histoHeight, axisPaint)

            // Draw Bars
            val barWidth = histoWidth / 10f
            val color = palette[index % palette.size]
            val barPaint = Paint().apply { this.color = color; style = Paint.Style.FILL }
            
            for (level in 1..10) {
                val count = counts[level] ?: 0
                val barHeight = (count.toFloat() / maxCount) * histoHeight
                val left = curX + (level - 1) * barWidth + 1f
                val top = state.y + histoHeight - barHeight
                val right = curX + level * barWidth - 1f
                val bottom = state.y + histoHeight
                
                if (barHeight > 0) {
                    state.canvas.drawRect(RectF(left, top, right, bottom), barPaint)
                }
            }

            // Draw labels for 1 and 10
            val labelPaint = Paint(textPaint).apply { textSize = 7f }
            state.canvas.drawText("1", curX, state.y + histoHeight + 10f, labelPaint)
            state.canvas.drawText("10", curX + histoWidth - 8f, state.y + histoHeight + 10f, labelPaint)

            curX += histoWidth + spacing
        }
        
        state.advance(histoHeight + 30f)
    }
}
