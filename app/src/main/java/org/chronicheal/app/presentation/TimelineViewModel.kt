package org.chronicheal.app.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
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
    private val reminderRepository: ReminderRepository,
    private val reminderScheduler: ReminderScheduler
) : ViewModel() {

    val uiState: StateFlow<TimelineUiState> = getEntriesUseCase()
        .map { TimelineUiState(entries = it) }
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
            addEntryUseCase(entry)
            val id = reminderRepository.insertReminder(reminder)
            val savedReminder = reminder.copy(id = id)
            if (savedReminder.isEnabled) {
                reminderScheduler.schedule(savedReminder)
            }
        }
    }

    fun updateEntry(entry: HealthEntry) {
        viewModelScope.launch {
            updateEntryUseCase(entry)
        }
    }

    fun deleteEntry(entry: HealthEntry) {
        viewModelScope.launch {
            deleteEntryUseCase(entry)
        }
    }

    suspend fun getEntryById(id: Long): HealthEntry? {
        return getEntryByIdUseCase(id)
    }
}

data class TimelineUiState(
    val entries: List<HealthEntry> = emptyList(),
    val isLoading: Boolean = false
)
