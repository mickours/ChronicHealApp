package org.chronicheal.app.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.chronicheal.app.data.notification.ReminderScheduler
import org.chronicheal.app.domain.model.Reminder
import org.chronicheal.app.domain.repository.ReminderRepository
import javax.inject.Inject

@HiltViewModel
class RemindersViewModel @Inject constructor(
    private val repository: ReminderRepository,
    private val scheduler: ReminderScheduler
) : ViewModel() {

    val reminders: StateFlow<List<Reminder>> = repository.getAllReminders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun toggleReminder(reminder: Reminder) {
        viewModelScope.launch {
            val updated = reminder.copy(isEnabled = !reminder.isEnabled)
            repository.updateReminder(updated)
            if (updated.isEnabled) {
                scheduler.schedule(updated)
            } else {
                scheduler.cancel(updated)
            }
        }
    }

    fun deleteReminder(reminder: Reminder) {
        viewModelScope.launch {
            scheduler.cancel(reminder)
            repository.deleteReminder(reminder)
        }
    }

    fun addReminder(reminder: Reminder) {
        viewModelScope.launch {
            val id = repository.insertReminder(reminder)
            val savedReminder = reminder.copy(id = id)
            if (savedReminder.isEnabled) {
                scheduler.schedule(savedReminder)
            }
        }
    }

    fun updateReminder(reminder: Reminder) {
        viewModelScope.launch {
            repository.updateReminder(reminder)
            if (reminder.isEnabled) {
                scheduler.schedule(reminder)
            } else {
                scheduler.cancel(reminder)
            }
        }
    }

    suspend fun getReminderById(id: Long): Reminder? {
        return repository.getReminderById(id)
    }
}
