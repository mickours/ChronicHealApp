package org.chronicheal.app.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.chronicheal.app.domain.model.EntryType
import org.chronicheal.app.domain.repository.SettingsRepository
import javax.inject.Inject

data class WelcomeWizardUiState(
    val favoriteTypes: Set<EntryType> = emptySet(),
    val isCompleted: Boolean = false
)

@HiltViewModel
class WelcomeWizardViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
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

    fun completeWizard() {
        viewModelScope.launch {
            settingsRepository.setFavoriteEntryTypes(_uiState.value.favoriteTypes)
            settingsRepository.setWelcomeWizardCompleted(true)
            _uiState.update { it.copy(isCompleted = true) }
        }
    }
}
