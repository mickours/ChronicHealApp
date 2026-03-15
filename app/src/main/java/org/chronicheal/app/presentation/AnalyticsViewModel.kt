package org.chronicheal.app.presentation

import android.net.Uri
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
import org.chronicheal.app.R
import org.chronicheal.app.domain.model.Allergen
import org.chronicheal.app.domain.model.EntryType
import org.chronicheal.app.domain.model.HealthEntry
import org.chronicheal.app.domain.usecase.ExportPdfUseCase
import org.chronicheal.app.domain.usecase.GetEntriesUseCase
import java.io.OutputStream
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.WeekFields
import java.util.Locale
import javax.inject.Inject
import kotlin.math.sqrt

sealed class CorrelationMetric {
    abstract val emoji: String
    abstract val labelRes: Int
    abstract val id: String

    data class Type(val entryType: EntryType) : CorrelationMetric() {
        override val emoji: String = entryType.emoji
        override val labelRes: Int = entryType.displayRes
        override val id: String = "type_${entryType.name}"
    }

    data class BeverageAttribute(val attribute: String, override val emoji: String, override val labelRes: Int) : CorrelationMetric() {
        override val id: String = "beverage_$attribute"
    }

    data class AllergenMetric(val allergen: Allergen) : CorrelationMetric() {
        override val emoji: String = "🚫"
        override val labelRes: Int = allergen.displayRes
        override val id: String = "allergen_${allergen.id}"
    }
}

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    getEntriesUseCase: GetEntriesUseCase,
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

    private val _pdfExportSuccess = MutableSharedFlow<Uri>()
    val pdfExportSuccess = _pdfExportSuccess.asSharedFlow()

    private val _correlationMetric1 = MutableStateFlow<CorrelationMetric>(CorrelationMetric.Type(EntryType.PAIN))
    val correlationMetric1 = _correlationMetric1.asStateFlow()

    private val _correlationMetric2 = MutableStateFlow<CorrelationMetric>(CorrelationMetric.Type(EntryType.SLEEP))
    val correlationMetric2 = _correlationMetric2.asStateFlow()

    private val _selectedPainLocations = MutableStateFlow<Set<String>>(emptySet())
    val selectedPainLocations = _selectedPainLocations.asStateFlow()

    private val _selectedSymptoms = MutableStateFlow<Set<String>>(emptySet())
    val selectedSymptoms = _selectedSymptoms.asStateFlow()

    val availableMetrics = EntryType.entries
        .filter { it != EntryType.VOICE_LOGGING }
        .map { CorrelationMetric.Type(it) } +
        listOf(
            CorrelationMetric.BeverageAttribute("alcoholic", "🍷", R.string.is_alcoholic),
            CorrelationMetric.BeverageAttribute("caffeinated", "☕", R.string.is_caffeinated)
        ) +
        Allergen.entries.map { CorrelationMetric.AllergenMetric(it) }

    val uiState: StateFlow<AnalyticsUiState> = combine(
        getEntriesUseCase(),
        _timeRange,
        _startDate,
        _correlationMetric1,
        _correlationMetric2
    ) { entries, range, start, metric1, metric2 ->
        val filteredEntries = filterEntries(entries, range, start)
        val painData = getPainData(filteredEntries, range, start)
        val symptomData = getSymptomData(filteredEntries, range, start)

        _selectedPainLocations.value = painData.keys
        _selectedSymptoms.value = symptomData.keys

        AnalyticsUiState(
            painData = painData,
            symptomEvolutionData = symptomData,
            correlationData = getCorrelationData(filteredEntries, range, start, metric1, metric2)
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

    fun setCorrelationMetrics(metric1: CorrelationMetric, metric2: CorrelationMetric) {
        _correlationMetric1.value = metric1
        _correlationMetric2.value = metric2
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
            TimeRange.ALL -> LocalDate.of(2000, 1, 1)
        }
    }

    fun movePeriod(direction: Int) {
        val current = _startDate.value
        _startDate.value = when (_timeRange.value) {
            TimeRange.WEEK -> current.plusWeeks(direction.toLong())
            TimeRange.MONTH -> current.plusMonths(direction.toLong())
            TimeRange.YEAR -> current.plusYears(direction.toLong())
            TimeRange.ALL -> current
        }
    }

    suspend fun onPdfExportRequested(outputStream: OutputStream, uri: Uri) {
        _isLoading.value = true
        try {
            val start = _startDate.value
            val end = when (_timeRange.value) {
                TimeRange.WEEK -> start.plusDays(6)
                TimeRange.MONTH -> start.plusMonths(1).minusDays(1)
                TimeRange.YEAR -> start.plusYears(1).minusDays(1)
                TimeRange.ALL -> LocalDate.now()
            }
            exportPdfUseCase(outputStream, start, end)
            _pdfExportSuccess.emit(uri)
        } catch (e: Exception) {
            _message.emit("Failed to export PDF: ${e.message}")
        } finally {
            _isLoading.value = false
        }
    }

    private fun filterEntries(entries: List<HealthEntry>, range: TimeRange, start: LocalDate): List<HealthEntry> {
        if (range == TimeRange.ALL) return entries
        
        val end = when (range) {
            TimeRange.WEEK -> start.plusDays(7)
            TimeRange.MONTH -> start.plusMonths(1)
            TimeRange.YEAR -> start.plusYears(1)
            else -> start.plusDays(1)
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
                val weekFields = WeekFields.of(Locale.getDefault())
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
                (1..12).associate { i ->
                    val targetMonth = start.plusMonths(i.toLong() - 1).month
                    val monthEntries = entries.filter { it.timestamp.atZone(ZoneId.systemDefault()).toLocalDate().month == targetMonth }
                    targetMonth.getDisplayName(java.time.format.TextStyle.SHORT, Locale.getDefault()) to if (monthEntries.isEmpty()) 0 else aggregator(monthEntries)
                }
            }
            TimeRange.ALL -> {
                if (entries.isEmpty()) return emptyMap()
                val sortedEntries = entries.sortedBy { it.timestamp }
                val firstYear = sortedEntries.first().timestamp.atZone(ZoneId.systemDefault()).year
                val lastYear = sortedEntries.last().timestamp.atZone(ZoneId.systemDefault()).year
                (firstYear..lastYear).associate { year ->
                    val yearEntries = entries.filter { it.timestamp.atZone(ZoneId.systemDefault()).year == year }
                    year.toString() to if (yearEntries.isEmpty()) 0 else aggregator(yearEntries)
                }
            }
        }
    }

    private fun getCorrelationData(
        entries: List<HealthEntry>,
        range: TimeRange,
        start: LocalDate,
        metric1: CorrelationMetric,
        metric2: CorrelationMetric
    ): CorrelationData {
        
        fun getValues(metric: CorrelationMetric): Map<String, Int> {
            return when (metric) {
                is CorrelationMetric.Type -> {
                    val typeEntries = entries.filter { it.type == metric.entryType }
                    aggregateData(typeEntries, range, start) { dayEntries ->
                        when (metric.entryType) {
                            EntryType.SLEEP -> {
                                dayEntries.mapNotNull { it.intensity?.toFloat() ?: it.durationMinutes?.toFloat()?.div(60f) }.average().toInt()
                            }
                            EntryType.PAIN, EntryType.SYMPTOM, EntryType.DISEASE, EntryType.EXTERNAL_FACTOR, EntryType.PERIOD, EntryType.MOOD -> {
                                dayEntries.mapNotNull { it.intensity?.toFloat() }.average().toInt()
                            }
                            EntryType.BEVERAGE -> {
                                dayEntries.sumOf { it.value ?: 0.0 }.toInt()
                            }
                            EntryType.DRUG, EntryType.MEAL, EntryType.ACTIVITY, EntryType.MEDICAL_APPOINTMENT, EntryType.JOURNAL, EntryType.STOOL, EntryType.VOICE_LOGGING -> {
                                if (metric.entryType == EntryType.ACTIVITY) dayEntries.sumOf { it.durationMinutes ?: 0 } / 60
                                else dayEntries.size
                            }
                        }
                    }
                }
                is CorrelationMetric.BeverageAttribute -> {
                    val bevEntries = entries.filter { it.type == EntryType.BEVERAGE }
                    val filteredBevs = when (metric.attribute) {
                        "alcoholic" -> bevEntries.filter { it.isAlcoholic == true }
                        "caffeinated" -> bevEntries.filter { it.isCaffeinated == true }
                        else -> bevEntries
                    }
                    aggregateData(filteredBevs, range, start) { dayEntries ->
                        dayEntries.sumOf { it.value ?: 0.0 }.toInt()
                    }
                }
                is CorrelationMetric.AllergenMetric -> {
                    val mealEntries = entries.filter { it.type == EntryType.MEAL && it.allergens?.contains(metric.allergen.id) == true }
                    aggregateData(mealEntries, range, start) { it.size }
                }
            }
        }

        val data1 = getValues(metric1)
        val data2 = getValues(metric2)
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
}

enum class TimeRange(val labelRes: Int) {
    WEEK(R.string.range_week), 
    MONTH(R.string.range_month), 
    YEAR(R.string.range_year),
    ALL(R.string.range_all)
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
