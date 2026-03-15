package org.chronicheal.app.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.chronicheal.app.data.notification.ReminderScheduler
import org.chronicheal.app.domain.model.EntryType
import org.chronicheal.app.domain.model.HealthEntry
import org.chronicheal.app.domain.model.Reminder
import org.chronicheal.app.domain.repository.EntryRepository
import org.chronicheal.app.domain.repository.ReminderRepository
import org.chronicheal.app.domain.repository.SettingsRepository
import org.chronicheal.app.domain.usecase.AddEntryUseCase
import org.chronicheal.app.domain.usecase.DeleteEntryUseCase
import org.chronicheal.app.domain.usecase.GetEntriesUseCase
import org.chronicheal.app.domain.usecase.GetEntryByIdUseCase
import org.chronicheal.app.domain.usecase.GetReminderByIdUseCase
import org.chronicheal.app.domain.usecase.UpdateEntryUseCase
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.inject.Inject

@HiltViewModel
class TimelineViewModel @Inject constructor(
    private val getEntriesUseCase: GetEntriesUseCase,
    private val addEntryUseCase: AddEntryUseCase,
    private val deleteEntryUseCase: DeleteEntryUseCase,
    private val updateEntryUseCase: UpdateEntryUseCase,
    private val getEntryByIdUseCase: GetEntryByIdUseCase,
    private val getReminderByIdUseCase: GetReminderByIdUseCase,
    private val entryRepository: EntryRepository,
    private val reminderRepository: ReminderRepository,
    private val reminderScheduler: ReminderScheduler,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private var recentlyDeletedEntry: HealthEntry? = null
    private var lastAction: UndoAction? = null

    private val _message = MutableStateFlow<String?>(null)
    private val _searchQuery = MutableStateFlow("")
    private val _selectedTypes = MutableStateFlow<Set<EntryType>>(emptySet())

    private val filterState = combine(_searchQuery, _selectedTypes) { query, types ->
        query to types
    }
    
    private val settingsState = combine(
        _message,
        settingsRepository.favoriteEntryTypes,
        settingsRepository.hasShownVoicePermissionRationale
    ) { message, favorites, hasShownVoiceRationale ->
        Triple(message, favorites, hasShownVoiceRationale)
    }

    val uiState: StateFlow<TimelineUiState> = combine(
        getEntriesUseCase(),
        filterState,
        settingsState
    ) { entries, filter, settings ->
        val (query, selectedTypes) = filter
        val (message, favorites, hasShownVoiceRationale) = settings
        
        val filteredEntries = entries.filter { entry ->
            val matchesQuery = query.isBlank() || 
                entry.name?.contains(query, ignoreCase = true) == true ||
                entry.location?.contains(query, ignoreCase = true) == true ||
                entry.origin?.contains(query, ignoreCase = true) == true ||
                    entry.note.contains(query, ignoreCase = true) == true
            
            val matchesType = selectedTypes.isEmpty() || entry.type in selectedTypes
            
            matchesQuery && matchesType
        }
        
        val weeklyStats = calculateWeeklyStats(entries)

        TimelineUiState(
            entries = filteredEntries, 
            searchQuery = query,
            selectedTypes = selectedTypes,
            message = message,
            favorites = favorites,
            weeklyStats = weeklyStats,
            hasShownVoiceRationale = hasShownVoiceRationale
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TimelineUiState()
    )

    val drugReminders: StateFlow<List<Reminder>> = reminderRepository.getEnabledReminders()
        .map { reminders -> reminders.filter { it.entryType == EntryType.DRUG } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val checkupReminders: StateFlow<List<Reminder>> = reminderRepository.getAllReminders()
        .map { reminders: List<Reminder> -> 
            reminders.filter { it.title == "Checkup" || it.title == "Daily Checkup" || it.title == "Complete Check-in" } 
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private fun calculateWeeklyStats(entries: List<HealthEntry>): WeeklyStats? {
        val now = Instant.now()
        val oneWeekAgo = now.minus(7, ChronoUnit.DAYS)
        val twoWeeksAgo = now.minus(14, ChronoUnit.DAYS)

        val currentWeekEntries = entries.filter { it.timestamp.isAfter(oneWeekAgo) }
        val previousWeekEntries = entries.filter { it.timestamp.isAfter(twoWeeksAgo) && it.timestamp.isBefore(oneWeekAgo) }

        if (currentWeekEntries.isEmpty()) return null

        val currentPain = currentWeekEntries.filter { it.type == EntryType.PAIN }.mapNotNull { it.intensity }.average().takeIf { !it.isNaN() }
        val previousPain = previousWeekEntries.filter { it.type == EntryType.PAIN }.mapNotNull { it.intensity }.average().takeIf { !it.isNaN() }
        
        val currentDrugs = currentWeekEntries.count { it.type == EntryType.DRUG }
        val previousDrugs = previousWeekEntries.count { it.type == EntryType.DRUG }

        val currentSleep = currentWeekEntries.filter { it.type == EntryType.SLEEP }.mapNotNull { it.durationMinutes?.toDouble() ?: (it.intensity?.toDouble()?.times(60.0)) }.average().takeIf { !it.isNaN() }
        val previousSleep = previousWeekEntries.filter { it.type == EntryType.SLEEP }.mapNotNull { it.durationMinutes?.toDouble() ?: (it.intensity?.toDouble()?.times(60.0)) }.average().takeIf { !it.isNaN() }

        return WeeklyStats(
            avgPain = currentPain,
            painTrend = if (currentPain != null && previousPain != null) ((currentPain - previousPain) / previousPain * 100).toInt() else null,
            drugCount = currentDrugs,
            drugTrend = if (previousDrugs > 0) currentDrugs - previousDrugs else null,
            avgSleepMinutes = currentSleep?.toInt(),
            sleepTrend = if (currentSleep != null && previousSleep != null) ((currentSleep - previousSleep) / previousSleep * 100).toInt() else null
        )
    }

    private fun createSortedSuggestions(
        filter: (HealthEntry) -> Boolean,
        selector: (HealthEntry) -> String?
    ): StateFlow<List<String>> {
        return getEntriesUseCase()
            .map { entries ->
                entries
                    .filter(filter)
                    .mapNotNull(selector)
                    .filter { it.isNotBlank() }
                    .groupBy { it }
                    .mapValues { it.value.size }
                    .toList()
                    .sortedByDescending { it.second }
                    .map { it.first }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    }

    val symptomSuggestions: StateFlow<List<String>> = createSortedSuggestions(
        filter = { it.type == EntryType.SYMPTOM },
        selector = { it.name }
    )

    val painLocationSuggestions: StateFlow<List<String>> = createSortedSuggestions(
        filter = { it.type == EntryType.PAIN || it.type == EntryType.SYMPTOM },
        selector = { it.location }
    )

    fun getPainOriginSuggestions(location: String): StateFlow<List<String>> {
        return getEntriesUseCase()
            .map { entries ->
                entries
                    .filter { it.type == EntryType.PAIN && (location.isBlank() || it.location == location) }
                    .mapNotNull { it.origin }
                    .filter { it.isNotBlank() }
                    .groupBy { it }
                    .mapValues { it.value.size }
                    .toList()
                    .sortedByDescending { it.second }
                    .map { it.first }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    }

    val drugSuggestions: StateFlow<List<String>> = createSortedSuggestions(
        filter = { it.type == EntryType.DRUG },
        selector = { it.name }
    )

    val activitySuggestions: StateFlow<List<String>> = createSortedSuggestions(
        filter = { it.type == EntryType.ACTIVITY },
        selector = { it.name }
    )

    val diseaseSuggestions: StateFlow<List<String>> = createSortedSuggestions(
        filter = { it.type == EntryType.DISEASE },
        selector = { it.name }
    )

    val doctorSuggestions: StateFlow<List<String>> = createSortedSuggestions(
        filter = { it.type == EntryType.MEDICAL_APPOINTMENT },
        selector = { it.name }
    )

    val mealSuggestions: StateFlow<List<String>> = createSortedSuggestions(
        filter = { it.type == EntryType.MEAL },
        selector = { it.name }
    )

    val externalFactorSuggestions: StateFlow<List<String>> = createSortedSuggestions(
        filter = { it.type == EntryType.EXTERNAL_FACTOR },
        selector = { it.name }
    )

    val beverageSuggestions: StateFlow<List<String>> = createSortedSuggestions(
        filter = { it.type == EntryType.BEVERAGE },
        selector = { it.name }
    )

    val stoolAspectSuggestions: StateFlow<List<String>> = createSortedSuggestions(
        filter = { it.type == EntryType.STOOL },
        selector = { it.name }
    )

    val allergenOrder: StateFlow<List<String>> = settingsRepository.allergenOrder
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val deactivatedAllergens: StateFlow<Set<String>> = settingsRepository.deactivatedAllergens
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val deactivatedFodmaps: StateFlow<Set<String>> = settingsRepository.deactivatedFodmaps
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleTypeFilter(type: EntryType) {
        val current = _selectedTypes.value
        if (type in current) {
            _selectedTypes.value = current - type
        } else {
            _selectedTypes.value = current + type
        }
    }

    fun toggleFavorite(type: EntryType) {
        viewModelScope.launch {
            val currentFavorites = settingsRepository.favoriteEntryTypes.first()
            val newFavorites = if (type in currentFavorites) {
                currentFavorites - type
            } else {
                currentFavorites + type
            }
            settingsRepository.setFavoriteEntryTypes(newFavorites)
        }
    }

    fun addEntry(entry: HealthEntry) {
        viewModelScope.launch {
            addEntryUseCase(entry)
        }
    }

    fun saveEntryAndNotify(original: HealthEntry?, current: HealthEntry) {
        if (original != null && original.id != 0L && original == current) return
        
        viewModelScope.launch {
            if (original == null || original.id == 0L) {
                // It's a new entry (or a new entry from a template)
                addEntryUseCase(current)
                val savedEntry = getEntriesUseCase().first().find { 
                    it.timestamp == current.timestamp && it.type == current.type 
                }
                savedEntry?.let {
                    lastAction = UndoAction.Add(it)
                    _message.value = "Entry added"
                }
            } else {
                updateEntryUseCase(current)
                lastAction = UndoAction.Update(original)
                _message.value = "Entry updated"
            }
        }
    }

    fun undoAction() {
        viewModelScope.launch {
            when (val action = lastAction) {
                is UndoAction.Add -> {
                    deleteEntryUseCase(action.entry)
                    lastAction = null
                    _message.value = "Addition undone"
                }
                is UndoAction.Update -> {
                    updateEntryUseCase(action.originalEntry)
                    lastAction = null
                    _message.value = "Changes undone"
                }
                else -> {}
            }
        }
    }

    fun addEntryWithReminder(entry: HealthEntry, reminder: Reminder) {
        viewModelScope.launch {
            val reminderId = reminderRepository.insertReminder(reminder)
            val savedReminder = reminder.copy(id = reminderId)
            if (savedReminder.isEnabled) {
                reminderScheduler.schedule(savedReminder)
            }
            addEntryUseCase(entry.copy(reminderId = reminderId))
        }
    }

    fun updateEntry(entry: HealthEntry) {
        viewModelScope.launch {
            updateEntryUseCase(entry)
        }
    }

    fun updateEntryWithReminder(entry: HealthEntry, reminder: Reminder) {
        viewModelScope.launch {
            val reminderId = reminderRepository.insertReminder(reminder)
            val savedReminder = reminder.copy(id = reminderId)
            if (savedReminder.isEnabled) {
                reminderScheduler.schedule(savedReminder)
            }
            updateEntryUseCase(entry.copy(reminderId = reminderId))
        }
    }

    fun saveReminder(reminder: Reminder) {
        viewModelScope.launch {
            val id = reminderRepository.insertReminder(reminder)
            if (reminder.isEnabled) {
                reminderScheduler.schedule(reminder.copy(id = id))
            } else {
                reminderScheduler.cancel(reminder.copy(id = id))
            }
        }
    }

    fun deleteEntry(entry: HealthEntry) {
        viewModelScope.launch {
            recentlyDeletedEntry = entry
            deleteEntryUseCase(entry)
            _message.value = "Entry deleted"
        }
    }

    suspend fun getEntryById(id: Long): HealthEntry? {
        return getEntryByIdUseCase(id)
    }

    suspend fun getEntryByReminderId(reminderId: Long): HealthEntry? {
        return entryRepository.getEntryByReminderId(reminderId)
    }

    suspend fun getLastEntryByTypeAndName(type: EntryType, name: String): HealthEntry? {
        return entryRepository.getLastEntryByTypeAndName(type, name)
    }

    suspend fun getReminderById(id: Long): Reminder? {
        return getReminderByIdUseCase(id)
    }

    fun showMessage(message: String) {
        _message.value = message
    }

    fun clearMessage() {
        _message.value = null
    }

    fun setHasShownVoiceRationale(shown: Boolean) {
        viewModelScope.launch {
            settingsRepository.setHasShownVoicePermissionRationale(shown)
        }
    }

    fun setAllergenOrder(order: List<String>) {
        viewModelScope.launch {
            settingsRepository.setAllergenOrder(order)
        }
    }
}

sealed class UndoAction {
    data class Add(val entry: HealthEntry) : UndoAction()
    data class Update(val originalEntry: HealthEntry) : UndoAction()
}

data class WeeklyStats(
    val avgPain: Double? = null,
    val painTrend: Int? = null, // percentage
    val drugCount: Int = 0,
    val drugTrend: Int? = null, // absolute difference
    val avgSleepMinutes: Int? = null,
    val sleepTrend: Int? = null // percentage
)

data class TimelineUiState(
    val entries: List<HealthEntry> = emptyList(),
    val searchQuery: String = "",
    val selectedTypes: Set<EntryType> = emptySet(),
    val isLoading: Boolean = false,
    val message: String? = null,
    val favorites: Set<EntryType> = emptySet(),
    val weeklyStats: WeeklyStats? = null,
    val hasShownVoiceRationale: Boolean = false
)
