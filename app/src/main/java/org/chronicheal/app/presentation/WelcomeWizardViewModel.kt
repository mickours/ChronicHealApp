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
    val favoriteTypes: Set<EntryType> = emptySet(), // Removed VOICE_LOGGING from default
    val isCheckupReminderEnabled: Boolean = false,
    val checkupReminderTime: LocalTime = LocalTime.of(20, 0), // Default 8 PM
    val isBiometricLockEnabled: Boolean = false,
    val isCompleted: Boolean = false,
    
    // Profile Fields
    val userAge: String = "",
    val userSex: String = "",
    val userWeight: String = "",
    val userHeight: String = "",
    val chronicDiseases: Set<String> = emptySet(),
    
    // Allergens
    val deactivatedAllergens: Set<String> = emptySet()
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

    fun setCheckupReminder(enabled: Boolean) {
        _uiState.update { it.copy(isCheckupReminderEnabled = enabled) }
    }

    fun setCheckupReminderTime(time: LocalTime) {
        _uiState.update { it.copy(checkupReminderTime = time) }
    }

    fun setBiometricLockEnabled(enabled: Boolean) {
        _uiState.update { it.copy(isBiometricLockEnabled = enabled) }
    }

    // Profile Updates
    fun setUserAge(age: String) { _uiState.update { it.copy(userAge = age) } }
    fun setUserSex(sex: String) { _uiState.update { it.copy(userSex = sex) } }
    fun setUserWeight(weight: String) { _uiState.update { it.copy(userWeight = weight) } }
    fun setUserHeight(height: String) { _uiState.update { it.copy(userHeight = height) } }
    
    fun toggleChronicDisease(disease: String) {
        _uiState.update { state ->
            val newDiseases = if (state.chronicDiseases.contains(disease)) {
                state.chronicDiseases - disease
            } else {
                state.chronicDiseases + disease
            }
            state.copy(chronicDiseases = newDiseases)
        }
    }
    
    fun addChronicDisease(disease: String) {
        if (disease.isBlank()) return
        _uiState.update { it.copy(chronicDiseases = it.chronicDiseases + disease) }
    }

    fun toggleAllergenDeactivation(allergen: String) {
        _uiState.update { state ->
            val current = state.deactivatedAllergens
            val next = if (allergen in current) current - allergen else current + allergen
            state.copy(deactivatedAllergens = next)
        }
    }

    fun completeWizard() {
        viewModelScope.launch {
            if (_uiState.value.isCheckupReminderEnabled) {
                val reminder = Reminder(
                    title = "Checkup",
                    time = _uiState.value.checkupReminderTime,
                    daysOfWeek = (1..7).toSet(),
                    isEnabled = true
                )
                val id = reminderRepository.insertReminder(reminder)
                reminderScheduler.schedule(reminder.copy(id = id))
            }
            
            securityRepository.setBiometricLockEnabled(_uiState.value.isBiometricLockEnabled)
            settingsRepository.setFavoriteEntryTypes(_uiState.value.favoriteTypes)
            
            // Save Profile Data
            settingsRepository.setUserAge(_uiState.value.userAge.toIntOrNull() ?: 0)
            settingsRepository.setUserSex(_uiState.value.userSex.ifBlank { null })
            settingsRepository.setUserWeight(_uiState.value.userWeight.replace(",", ".").toFloatOrNull() ?: 0f)
            settingsRepository.setUserHeight(_uiState.value.userHeight.toIntOrNull() ?: 0)
            settingsRepository.setChronicDiseases(_uiState.value.chronicDiseases)
            
            // Save Allergens
            settingsRepository.setDeactivatedAllergens(_uiState.value.deactivatedAllergens)

            settingsRepository.setWelcomeWizardCompleted(true)
            _uiState.update { it.copy(isCompleted = true) }
        }
    }
}
