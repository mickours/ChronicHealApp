package org.chronicheal.app.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.chronicheal.app.data.notification.ReminderScheduler
import org.chronicheal.app.domain.model.HealthEntry
import org.chronicheal.app.domain.model.Reminder
import org.chronicheal.app.domain.repository.ReminderRepository
import org.chronicheal.app.domain.usecase.*
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
    private val reminderScheduler: ReminderScheduler
) : ViewModel() {

    private var recentlyDeletedEntry: HealthEntry? = null
    private val _message = MutableStateFlow<String?>(null)

    val uiState: StateFlow<TimelineUiState> = combine(
        getEntriesUseCase(),
        _message
    ) { entries, message ->
        TimelineUiState(entries = entries, message = message)
    }
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TimelineUiState()
    )

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

    fun showMessage(msg: String) {
        _message.value = msg
    }

    fun clearMessage() {
        _message.value = null
    }

    suspend fun getEntryById(id: Long): HealthEntry? {
        return getEntryByIdUseCase(id)
    }

    suspend fun getReminderById(id: Long): Reminder? {
        return getReminderByIdUseCase(id)
    }
}

data class TimelineUiState(
    val entries: List<HealthEntry> = emptyList(),
    val isLoading: Boolean = false,
    val message: String? = null
)
