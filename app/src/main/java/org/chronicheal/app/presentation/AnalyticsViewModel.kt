package org.chronicheal.app.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.chronicheal.app.domain.model.EntryType
import org.chronicheal.app.domain.model.HealthEntry
import org.chronicheal.app.domain.usecase.ExportPdfUseCase
import org.chronicheal.app.domain.usecase.GetEntriesUseCase
import java.io.OutputStream
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
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

    val uiState: StateFlow<AnalyticsUiState> = combine(
        getEntriesUseCase(),
        _timeRange,
        _startDate
    ) { entries, range, start ->
        val filteredEntries = filterEntries(entries, range, start)
        AnalyticsUiState(
            painData = getPainData(filteredEntries, range, start),
            symptomFrequency = getSymptomFrequency(filteredEntries)
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

    private fun getPainData(entries: List<HealthEntry>, range: TimeRange, start: LocalDate): Map<LocalDate, Int> {
        val data = entries
            .filter { it.type == EntryType.PAIN && it.intensity != null }
            .groupBy { it.timestamp.atZone(ZoneId.systemDefault()).toLocalDate() }
            .mapValues { (_, dayEntries) ->
                dayEntries.mapNotNull { it.intensity }.average().toInt()
            }
        
        val result = mutableMapOf<LocalDate, Int>()
        val daysCount = when (range) {
            TimeRange.WEEK -> 7L
            TimeRange.MONTH -> ChronoUnit.DAYS.between(start, start.plusMonths(1))
            TimeRange.YEAR -> ChronoUnit.DAYS.between(start, start.plusYears(1))
        }
        
        for (i in 0 until daysCount) {
            val date = start.plusDays(i)
            result[date] = data[date] ?: 0
        }
        
        return result.toSortedMap()
    }

    private fun getSymptomFrequency(entries: List<HealthEntry>): Map<String, Int> {
        return entries
            .filter { it.type == EntryType.SYMPTOM && it.name != null }
            .groupBy { it.name!! }
            .mapValues { it.value.size }
            .toList()
            .sortedByDescending { it.second }
            .take(5)
            .toMap()
    }
}

enum class TimeRange {
    WEEK, MONTH, YEAR
}

data class AnalyticsUiState(
    val painData: Map<LocalDate, Int> = emptyMap(),
    val symptomFrequency: Map<String, Int> = emptyMap()
)
