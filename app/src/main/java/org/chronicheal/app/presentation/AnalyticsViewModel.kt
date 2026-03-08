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
import java.time.temporal.WeekFields
import java.util.Locale
import javax.inject.Inject
import kotlin.math.sqrt

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

    private val _selectedPainLocations = MutableStateFlow<Set<String>>(emptySet())
    val selectedPainLocations = _selectedPainLocations.asStateFlow()

    private val _selectedSymptoms = MutableStateFlow<Set<String>>(emptySet())
    val selectedSymptoms = _selectedSymptoms.asStateFlow()

    val uiState: StateFlow<AnalyticsUiState> = combine(
        getEntriesUseCase(),
        _timeRange,
        _startDate,
        _correlationType1,
        _correlationType2
    ) { entries, range, start, type1, type2 ->
        val filteredEntries = filterEntries(entries, range, start)
        val painData = getPainData(filteredEntries, range, start)
        val symptomData = getSymptomData(filteredEntries, range, start)

        _selectedPainLocations.value = painData.keys
        _selectedSymptoms.value = symptomData.keys

        AnalyticsUiState(
            painData = painData,
            symptomEvolutionData = symptomData,
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

    fun togglePainLocation(location: String) {
        val current = _selectedPainLocations.value
        _selectedPainLocations.value = if (location in current) current - location else current + location
    }

    fun toggleSymptom(symptom: String) {
        val current = _selectedSymptoms.value
        _selectedSymptoms.value = if (symptom in current) current - symptom else current + symptom
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

    private fun getPainData(entries: List<HealthEntry>, range: TimeRange, start: LocalDate): Map<String, Map<String, Int>> {
        val painEntries = entries.filter { it.type == EntryType.PAIN && it.intensity != null }
        
        val normalizedPainEntries = painEntries.map { entry ->
            val rawLocation = entry.location?.trim()?.lowercase() ?: ""
            val normalizedLocation = if (rawLocation.isBlank()) "General" 
                                     else rawLocation.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
            entry.copy(location = normalizedLocation)
        }

        val locations = normalizedPainEntries.map { it.location!! }.distinct()
        
        return locations.associateWith { location ->
            val locationEntries = normalizedPainEntries.filter { it.location == location }
            aggregateData(locationEntries, range, start) { it.mapNotNull { e -> e.intensity }.average().toInt() }
        }
    }

    private fun getSymptomData(entries: List<HealthEntry>, range: TimeRange, start: LocalDate): Map<String, Map<String, Int>> {
        val symptomEntries = entries.filter { it.type == EntryType.SYMPTOM && it.intensity != null && !it.name.isNullOrBlank() }
        val symptoms = symptomEntries.map { it.name!! }.distinct()

        return symptoms.associateWith { symptom ->
            val symptomEntriesFiltered = symptomEntries.filter { it.name == symptom }
            aggregateData(symptomEntriesFiltered, range, start) { it.mapNotNull { e -> e.intensity }.average().toInt() }
        }
    }

    private fun aggregateData(
        entries: List<HealthEntry>,
        range: TimeRange,
        start: LocalDate,
        aggregator: (List<HealthEntry>) -> Int
    ): Map<String, Int> {
        return when (range) {
            TimeRange.WEEK -> {
                (0 until 7).associate { i ->
                    val date = start.plusDays(i.toLong())
                    val dayEntries = entries.filter { it.timestamp.atZone(ZoneId.systemDefault()).toLocalDate() == date }
                    date.dayOfWeek.getDisplayName(java.time.format.TextStyle.SHORT, Locale.getDefault()) to if (dayEntries.isEmpty()) 0 else aggregator(dayEntries)
                }
            }
            TimeRange.MONTH -> {
                // Group by week of month
                val weekFields = WeekFields.of(Locale.getDefault())
                val dataByWeek = entries.groupBy { it.timestamp.atZone(ZoneId.systemDefault()).toLocalDate().get(weekFields.weekOfMonth()) }
                
                // Usually 4-6 weeks in a month
                val result = mutableMapOf<String, Int>()
                val firstDay = start
                val lastDay = start.plusMonths(1).minusDays(1)
                var current = firstDay
                var weekNum = 1
                while (!current.isAfter(lastDay)) {
                    val currentWeekNum = current.get(weekFields.weekOfMonth())
                    val weekEntries = entries.filter { it.timestamp.atZone(ZoneId.systemDefault()).toLocalDate().get(weekFields.weekOfMonth()) == currentWeekNum }
                    result["W$weekNum"] = if (weekEntries.isEmpty()) 0 else aggregator(weekEntries)
                    current = current.plusWeeks(1)
                    weekNum++
                }
                result
            }
            TimeRange.YEAR -> {
                // Group by month
                (1..12).associate { i ->
                    val monthDate = start.withMonth(i) // This might be wrong if start is not Jan 1st
                    // Better: start is some date, we want 12 months from start
                    val targetMonth = start.plusMonths(i.toLong() - 1).month
                    val monthEntries = entries.filter { it.timestamp.atZone(ZoneId.systemDefault()).toLocalDate().month == targetMonth }
                    targetMonth.getDisplayName(java.time.format.TextStyle.SHORT, Locale.getDefault()) to if (monthEntries.isEmpty()) 0 else aggregator(monthEntries)
                }
            }
        }
    }

    private fun getCorrelationData(
        entries: List<HealthEntry>,
        range: TimeRange,
        start: LocalDate,
        type1: EntryType,
        type2: EntryType
    ): CorrelationData {
        
        fun getValues(type: EntryType): Map<String, Int> {
            val typeEntries = entries.filter { it.type == type }
            return aggregateData(typeEntries, range, start) { dayEntries ->
                when (type) {
                    EntryType.SLEEP -> {
                        dayEntries.mapNotNull { it.intensity?.toFloat() ?: it.durationMinutes?.toFloat()?.div(60f) }.average().toInt()
                    }
                    EntryType.PAIN, EntryType.SYMPTOM, EntryType.DISEASE, EntryType.EXTERNAL_FACTOR, EntryType.PERIOD, EntryType.MOOD -> {
                        dayEntries.mapNotNull { it.intensity?.toFloat() }.average().toInt()
                    }
                    EntryType.BEVERAGE -> {
                        dayEntries.sumOf { it.value ?: 0.0 }.toInt()
                    }
                    EntryType.DRUG, EntryType.MEAL, EntryType.ACTIVITY, EntryType.MEDICAL_APPOINTMENT, EntryType.JOURNAL, EntryType.STOOL -> {
                        if (type == EntryType.ACTIVITY) dayEntries.sumOf { it.durationMinutes ?: 0 } / 60
                        else dayEntries.size
                    }
                }
            }
        }

        val data1 = getValues(type1)
        val data2 = getValues(type2)
        val labels = data1.keys.toList()
        val series1 = data1.values.map { it.toFloat() }
        val series2 = data2.values.map { it.toFloat() }

        val pearsonCorrelation = calculatePearsonCorrelation(series1, series2)

        return CorrelationData(
            labels = labels,
            series1 = series1,
            series2 = series2,
            pearsonCorrelation = pearsonCorrelation
        )
    }

    private fun calculatePearsonCorrelation(x: List<Float>, y: List<Float>): Double? {
        if (x.size != y.size || x.size < 2) return null
        
        // Filter out pairs where at least one value is 0 (assuming 0 means no data for that period)
        val pairs = x.zip(y).filter { it.first != 0f || it.second != 0f }
        if (pairs.size < 2) return null

        val filteredX = pairs.map { it.first }
        val filteredY = pairs.map { it.second }

        val n = filteredX.size
        val sumX = filteredX.sum()
        val sumY = filteredY.sum()
        val sumX2 = filteredX.sumOf { (it * it).toDouble() }
        val sumY2 = filteredY.sumOf { (it * it).toDouble() }
        val sumXY = filteredX.zip(filteredY).sumOf { (it.first * it.second).toDouble() }

        val numerator = n * sumXY - sumX * sumY
        val denominator = sqrt((n * sumX2 - sumX * sumX) * (n * sumY2 - sumY * sumY))

        return if (denominator == 0.0) null else numerator / denominator
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
    val painData: Map<String, Map<String, Int>> = emptyMap(),
    val symptomEvolutionData: Map<String, Map<String, Int>> = emptyMap(),
    val correlationData: CorrelationData = CorrelationData()
)

data class CorrelationData(
    val labels: List<String> = emptyList(),
    val series1: List<Float> = emptyList(),
    val series2: List<Float> = emptyList(),
    val pearsonCorrelation: Double? = null
)
