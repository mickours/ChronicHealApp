package org.chronicheal.app.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.chronicheal.app.data.notification.ReminderScheduler
import org.chronicheal.app.domain.model.EntryType
import org.chronicheal.app.domain.model.Reminder
import org.chronicheal.app.domain.repository.ReminderRepository
import org.chronicheal.app.domain.repository.SecurityRepository
import org.chronicheal.app.domain.repository.SettingsRepository
import java.time.LocalTime
import javax.inject.Inject

data class WelcomeWizardUiState(
    val favoriteTypes: Set<EntryType> = setOf(EntryType.VOICE_LOGGING),
    val isBodyScanReminderEnabled: Boolean = false,
    val bodyScanReminderTime: LocalTime = LocalTime.of(20, 0), // Default 8 PM
    val isBiometricLockEnabled: Boolean = false,
    val isCompleted: Boolean = false
)

@HiltViewModel
class WelcomeWizardViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val reminderRepository: ReminderRepository,
    private val reminderScheduler: ReminderScheduler,
    private val securityRepository: SecurityRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(WelcomeWizardUiState())
    val uiState: StateFlow<WelcomeWizardUiState> = _uiState.asStateFlow()

    fun toggleFavorite(type: EntryType) {
        _uiState.update { state ->
            val newFavorites = if (state.favoriteTypes.contains(type)) {
                state.favoriteTypes - type
            } else {
                state.favoriteTypes + type
            }
            state.copy(favoriteTypes = newFavorites)
        }
    }

    fun setBodyScanReminder(enabled: Boolean) {
        _uiState.update { it.copy(isBodyScanReminderEnabled = enabled) }
    }

    fun setBodyScanReminderTime(time: LocalTime) {
        _uiState.update { it.copy(bodyScanReminderTime = time) }
    }

    fun setBiometricLockEnabled(enabled: Boolean) {
        _uiState.update { it.copy(isBiometricLockEnabled = enabled) }
    }

    fun completeWizard() {
        viewModelScope.launch {
            if (_uiState.value.isBodyScanReminderEnabled) {
                val reminder = Reminder(
                    title = "Daily Body Scan",
                    time = _uiState.value.bodyScanReminderTime,
                    daysOfWeek = (1..7).toSet(),
                    entryType = EntryType.PAIN,
                    isEnabled = true
                )
                val id = reminderRepository.insertReminder(reminder)
                reminderScheduler.schedule(reminder.copy(id = id))
            }
            
            securityRepository.setBiometricLockEnabled(_uiState.value.isBiometricLockEnabled)
            settingsRepository.setFavoriteEntryTypes(_uiState.value.favoriteTypes)
            settingsRepository.setWelcomeWizardCompleted(true)
            _uiState.update { it.copy(isCompleted = true) }
        }
    }
}
