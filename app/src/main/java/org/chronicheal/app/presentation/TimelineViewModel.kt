package org.chronicheal.app.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.chronicheal.app.data.notification.ReminderScheduler
import org.chronicheal.app.domain.model.EntryType
import org.chronicheal.app.domain.model.HealthEntry
import org.chronicheal.app.domain.model.Reminder
import org.chronicheal.app.domain.repository.ReminderRepository
import org.chronicheal.app.domain.repository.SettingsRepository
import org.chronicheal.app.domain.usecase.AddEntryUseCase
import org.chronicheal.app.domain.usecase.DeleteEntryUseCase
import org.chronicheal.app.domain.usecase.GetEntriesUseCase
import org.chronicheal.app.domain.usecase.GetEntryByIdUseCase
import org.chronicheal.app.domain.usecase.GetReminderByIdUseCase
import org.chronicheal.app.domain.usecase.UpdateEntryUseCase
import javax.inject.Inject

@HiltViewModel
class TimelineViewModel @Inject constructor(
    private val getEntriesUseCase: GetEntriesUseCase,
    private val addEntryUseCase: AddEntryUseCase,
    private val deleteEntryUseCase: DeleteEntryUseCase,
    private val updateEntryUseCase: UpdateEntryUseCase,
    private val getEntryByIdUseCase: GetEntryByIdUseCase,
    private val getReminderByIdUseCase: GetReminderByIdUseCase,
    private val reminderRepository: ReminderRepository,
    private val reminderScheduler: ReminderScheduler,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private var recentlyDeletedEntry: HealthEntry? = null

    private val _message = MutableStateFlow<String?>(null)
    private val _searchQuery = MutableStateFlow("")
    private val _selectedTypes = MutableStateFlow<Set<EntryType>>(emptySet())

    val uiState: StateFlow<TimelineUiState> = combine(
        getEntriesUseCase(),
        _searchQuery,
        _selectedTypes,
        _message,
        settingsRepository.favoriteEntryTypes
    ) { entries, query, selectedTypes, message, favorites ->
        val filteredEntries = entries.filter { entry ->
            val matchesQuery = query.isBlank() || 
                entry.name?.contains(query, ignoreCase = true) == true ||
                entry.location?.contains(query, ignoreCase = true) == true ||
                entry.note?.contains(query, ignoreCase = true) == true
            
            val matchesType = selectedTypes.isEmpty() || entry.type in selectedTypes
            
            matchesQuery && matchesType
        }
        TimelineUiState(
            entries = filteredEntries, 
            searchQuery = query,
            selectedTypes = selectedTypes,
            message = message,
            favorites = favorites
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TimelineUiState()
    )

    val symptomSuggestions: StateFlow<List<String>> = getEntriesUseCase()
        .map { entries ->
            entries
                .filter { it.type == EntryType.SYMPTOM && !it.name.isNullOrBlank() }
                .mapNotNull { it.name }
                .distinct()
                .sorted()
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val painLocationSuggestions: StateFlow<List<String>> = getEntriesUseCase()
        .map { entries ->
            entries
                .filter { (it.type == EntryType.PAIN || it.type == EntryType.SYMPTOM) && !it.location.isNullOrBlank() }
                .mapNotNull { it.location }
                .distinct()
                .sorted()
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val drugSuggestions: StateFlow<List<String>> = getEntriesUseCase()
        .map { entries ->
            entries
                .filter { it.type == EntryType.DRUG && !it.name.isNullOrBlank() }
                .mapNotNull { it.name }
                .distinct()
                .sorted()
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activitySuggestions: StateFlow<List<String>> = getEntriesUseCase()
        .map { entries ->
            entries
                .filter { it.type == EntryType.ACTIVITY && !it.name.isNullOrBlank() }
                .mapNotNull { it.name }
                .distinct()
                .sorted()
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val diseaseSuggestions: StateFlow<List<String>> = getEntriesUseCase()
        .map { entries ->
            entries
                .filter { it.type == EntryType.DISEASE && !it.name.isNullOrBlank() }
                .mapNotNull { it.name }
                .distinct()
                .sorted()
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val doctorSuggestions: StateFlow<List<String>> = getEntriesUseCase()
        .map { entries ->
            entries
                .filter { it.type == EntryType.MEDICAL_APPOINTMENT && !it.name.isNullOrBlank() }
                .mapNotNull { it.name }
                .distinct()
                .sorted()
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val mealSuggestions: StateFlow<List<String>> = getEntriesUseCase()
        .map { entries ->
            entries
                .filter { it.type == EntryType.MEAL && !it.name.isNullOrBlank() }
                .mapNotNull { it.name }
                .distinct()
                .sorted()
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val externalFactorSuggestions: StateFlow<List<String>> = getEntriesUseCase()
        .map { entries ->
            entries
                .filter { it.type == EntryType.EXTERNAL_FACTOR && !it.name.isNullOrBlank() }
                .mapNotNull { it.name }
                .distinct()
                .sorted()
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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

    fun addEntry(entry: HealthEntry) {
        viewModelScope.launch {
            addEntryUseCase(entry)
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

    fun deleteEntry(entry: HealthEntry) {
        viewModelScope.launch {
            recentlyDeletedEntry = entry
            deleteEntryUseCase(entry)
        }
    }

    fun restoreDeletedEntry() {
        viewModelScope.launch {
            recentlyDeletedEntry?.let {
                addEntryUseCase(it)
                recentlyDeletedEntry = null
            }
        }
    }

    suspend fun getEntryById(id: Long): HealthEntry? {
        return getEntryByIdUseCase(id)
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
}

data class TimelineUiState(
    val entries: List<HealthEntry> = emptyList(),
    val searchQuery: String = "",
    val selectedTypes: Set<EntryType> = emptySet(),
    val isLoading: Boolean = false,
    val message: String? = null,
    val favorites: Set<EntryType> = emptySet()
)
