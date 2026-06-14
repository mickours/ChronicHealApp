package org.chronicheal.app.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.chronicheal.app.domain.model.EntryType
import org.chronicheal.app.domain.model.HealthEntry
import org.chronicheal.app.domain.model.Reminder
import org.chronicheal.app.domain.repository.ReminderRepository
import org.chronicheal.app.domain.usecase.DeleteReminderUseCase
import org.chronicheal.app.domain.usecase.GetEntryByIdUseCase
import org.chronicheal.app.domain.usecase.GetReminderByIdUseCase
import org.chronicheal.app.domain.usecase.GetSuggestionsUseCase
import org.chronicheal.app.domain.usecase.SaveReminderUseCase
import org.chronicheal.app.domain.usecase.ToggleReminderUseCase
import javax.inject.Inject

@HiltViewModel
class RemindersViewModel @Inject constructor(
    private val reminderRepository: ReminderRepository,
    private val getReminderByIdUseCase: GetReminderByIdUseCase,
    private val getEntryByIdUseCase: GetEntryByIdUseCase,
    private val getSuggestionsUseCase: GetSuggestionsUseCase,
    private val saveReminderUseCase: SaveReminderUseCase,
    private val deleteReminderUseCase: DeleteReminderUseCase,
    private val toggleReminderUseCase: ToggleReminderUseCase
) : ViewModel() {

    val reminders: StateFlow<List<Reminder>> = reminderRepository.getAllReminders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun toggleReminder(reminder: Reminder) {
        viewModelScope.launch {
            toggleReminderUseCase(reminder)
        }
    }

    fun deleteReminder(reminder: Reminder) {
        viewModelScope.launch {
            deleteReminderUseCase(reminder)
        }
    }

    fun addReminder(reminder: Reminder, templateEntry: HealthEntry? = null) {
        viewModelScope.launch {
            saveReminderUseCase(reminder, templateEntry)
        }
    }

    fun updateReminder(reminder: Reminder, templateEntry: HealthEntry? = null) {
        viewModelScope.launch {
            saveReminderUseCase(reminder, templateEntry)
        }
    }

    suspend fun getReminderById(id: Long): Reminder? {
        return getReminderByIdUseCase(id)
    }

    suspend fun getEntryById(id: Long): HealthEntry? {
        return getEntryByIdUseCase(id)
    }

    fun getNameSuggestions(type: EntryType) = getSuggestionsUseCase.execute(
        types = setOf(type),
        field = GetSuggestionsUseCase.SuggestionField.NAME
    ).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun getLocationSuggestions(type: EntryType) = getSuggestionsUseCase.execute(
        types = setOf(type),
        field = GetSuggestionsUseCase.SuggestionField.LOCATION
    ).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun getUnitSuggestions(type: EntryType) = getSuggestionsUseCase.execute(
        types = setOf(type),
        field = GetSuggestionsUseCase.SuggestionField.UNIT
    ).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
