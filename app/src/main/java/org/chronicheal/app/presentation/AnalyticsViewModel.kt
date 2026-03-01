package org.chronicheal.app.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.chronicheal.app.domain.model.EntryType
import org.chronicheal.app.domain.model.HealthEntry
import org.chronicheal.app.domain.usecase.ExportPdfUseCase
import org.chronicheal.app.domain.usecase.GetEntriesUseCase
import java.io.OutputStream
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val getEntriesUseCase: GetEntriesUseCase,
    private val exportPdfUseCase: ExportPdfUseCase
) : ViewModel() {

    private val _timeRange = MutableStateFlow(TimeRange.WEEK)
    val timeRange = _timeRange.asStateFlow()

    private val _startDate = MutableStateFlow(LocalDate.now().minusDays(6))
    val startDate = _startDate.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _message = MutableSharedFlow<String>()
    val message = _message.asSharedFlow()

    private val _correlationType1 = MutableStateFlow(EntryType.PAIN)
    val correlationType1 = _correlationType1.asStateFlow()

    private val _correlationType2 = MutableStateFlow(EntryType.SLEEP)
    val correlationType2 = _correlationType2.asStateFlow()

    val uiState: StateFlow<AnalyticsUiState> = combine(
        getEntriesUseCase(),
        _timeRange,
        _startDate,
        _correlationType1,
        _correlationType2
    ) { entries, range, start, type1, type2 ->
        val filteredEntries = filterEntries(entries, range, start)
        AnalyticsUiState(
            painData = getPainData(filteredEntries, range, start),
            symptomSeveritySum = getSymptomSeveritySum(filteredEntries),
            correlationData = getCorrelationData(filteredEntries, range, start, type1, type2)
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AnalyticsUiState()
    )

    fun setTimeRange(range: TimeRange) {
        _timeRange.value = range
        resetStartDate()
    }

    fun setCorrelationTypes(type1: EntryType, type2: EntryType) {
        _correlationType1.value = type1
        _correlationType2.value = type2
    }

    private fun resetStartDate() {
        _startDate.value = when (_timeRange.value) {
            TimeRange.WEEK -> LocalDate.now().minusDays(6)
            TimeRange.MONTH -> LocalDate.now().minusMonths(1)
            TimeRange.YEAR -> LocalDate.now().minusYears(1)
        }
    }

    fun movePeriod(direction: Int) {
        val current = _startDate.value
        _startDate.value = when (_timeRange.value) {
            TimeRange.WEEK -> current.plusWeeks(direction.toLong())
            TimeRange.MONTH -> current.plusMonths(direction.toLong())
            TimeRange.YEAR -> current.plusYears(direction.toLong())
        }
    }

    suspend fun exportPdf(outputStream: OutputStream) {
        _isLoading.value = true
        try {
            val start = _startDate.value
            val end = when (_timeRange.value) {
                TimeRange.WEEK -> start.plusDays(6)
                TimeRange.MONTH -> start.plusMonths(1).minusDays(1)
                TimeRange.YEAR -> start.plusYears(1).minusDays(1)
            }
            exportPdfUseCase(outputStream, start, end)
            _message.emit("PDF exported successfully")
        } catch (e: Exception) {
            _message.emit("Failed to export PDF: ${e.message}")
        } finally {
            _isLoading.value = false
        }
    }

    private fun filterEntries(entries: List<HealthEntry>, range: TimeRange, start: LocalDate): List<HealthEntry> {
        val end = when (range) {
            TimeRange.WEEK -> start.plusDays(7)
            TimeRange.MONTH -> start.plusMonths(1)
            TimeRange.YEAR -> start.plusYears(1)
        }
        return entries.filter {
            val entryDate = it.timestamp.atZone(ZoneId.systemDefault()).toLocalDate()
            !entryDate.isBefore(start) && entryDate.isBefore(end)
        }
    }

    private fun getPainData(entries: List<HealthEntry>, range: TimeRange, start: LocalDate): Map<String, Map<LocalDate, Int>> {
        val painEntries = entries.filter { it.type == EntryType.PAIN && it.intensity != null }
        
        val normalizedPainEntries = painEntries.map { entry ->
            val rawLocation = entry.location?.trim()?.lowercase() ?: ""
            val normalizedLocation = if (rawLocation.isBlank()) "General" 
                                     else rawLocation.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
            entry.copy(location = normalizedLocation)
        }

        val locations = normalizedPainEntries.map { it.location!! }.distinct()
        
        val daysCount = getDaysCount(range, start)

        return locations.associateWith { location ->
            val locationData = normalizedPainEntries
                .filter { it.location == location }
                .groupBy { it.timestamp.atZone(ZoneId.systemDefault()).toLocalDate() }
                .mapValues { (_, dayEntries) ->
                    dayEntries.mapNotNull { it.intensity }.average().toInt()
                }
            
            val result = mutableMapOf<LocalDate, Int>()
            for (i in 0 until daysCount) {
                val date = start.plusDays(i)
                result[date] = locationData[date] ?: 0
            }
            result.toSortedMap()
        }
    }

    private fun getSymptomSeveritySum(entries: List<HealthEntry>): Map<String, Int> {
        return entries
            .filter { it.type == EntryType.SYMPTOM && !it.name.isNullOrBlank() }
            .groupBy { 
                it.name!!.trim().lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
            }
            .mapValues { entry -> 
                entry.value.sumOf { it.intensity ?: 0 }
            }
            .toList()
            .sortedByDescending { it.second }
            .take(5)
            .toMap()
    }

    private fun getCorrelationData(
        entries: List<HealthEntry>,
        range: TimeRange,
        start: LocalDate,
        type1: EntryType,
        type2: EntryType
    ): CorrelationData {
        val daysCount = getDaysCount(range, start)
        val dates = (0 until daysCount).map { start.plusDays(it) }

        fun getValues(type: EntryType): List<Float> {
            val typeEntries = entries.filter { it.type == type }
            val dataByDate = typeEntries.groupBy { it.timestamp.atZone(ZoneId.systemDefault()).toLocalDate() }
            
            return dates.map { date ->
                val dayEntries = dataByDate[date] ?: emptyList()
                if (dayEntries.isEmpty()) 0f
                else {
                    when (type) {
                        EntryType.SLEEP -> {
                            // Average quality or duration? Let's use intensity (quality) if available, otherwise duration
                            dayEntries.mapNotNull { it.intensity?.toFloat() ?: it.durationMinutes?.toFloat()?.div(60f) }.average().toFloat()
                        }
                        EntryType.PAIN, EntryType.SYMPTOM, EntryType.DISEASE, EntryType.EXTERNAL_FACTOR -> {
                            dayEntries.mapNotNull { it.intensity?.toFloat() }.average().toFloat()
                        }
                        EntryType.DRUG, EntryType.MEAL, EntryType.ACTIVITY, EntryType.MEDICAL_APPOINTMENT, EntryType.JOURNAL -> {
                            // Frequency or duration
                            if (type == EntryType.ACTIVITY) dayEntries.sumOf { it.durationMinutes ?: 0 }.toFloat() / 60f
                            else dayEntries.size.toFloat()
                        }
                    }
                }
            }
        }

        return CorrelationData(
            dates = dates,
            series1 = getValues(type1),
            series2 = getValues(type2)
        )
    }

    private fun getDaysCount(range: TimeRange, start: LocalDate): Long {
        return when (range) {
            TimeRange.WEEK -> 7L
            TimeRange.MONTH -> ChronoUnit.DAYS.between(start, start.plusMonths(1))
            TimeRange.YEAR -> ChronoUnit.DAYS.between(start, start.plusYears(1))
        }
    }
}

enum class TimeRange {
    WEEK, MONTH, YEAR
}

data class AnalyticsUiState(
    val painData: Map<String, Map<LocalDate, Int>> = emptyMap(),
    val symptomSeveritySum: Map<String, Int> = emptyMap(),
    val correlationData: CorrelationData = CorrelationData()
)

data class CorrelationData(
    val dates: List<LocalDate> = emptyList(),
    val series1: List<Float> = emptyList(),
    val series2: List<Float> = emptyList()
)
