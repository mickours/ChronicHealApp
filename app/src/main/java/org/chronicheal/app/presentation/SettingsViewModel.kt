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
import org.chronicheal.app.data.worker.BackupManager
import org.chronicheal.app.domain.repository.SettingsRepository
import org.chronicheal.app.domain.usecase.ExportDataUseCase
import org.chronicheal.app.domain.usecase.ImportDataUseCase
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val exportDataUseCase: ExportDataUseCase,
    private val importDataUseCase: ImportDataUseCase,
    private val settingsRepository: SettingsRepository,
    private val backupManager: BackupManager
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    private val _message = MutableStateFlow<String?>(null)

    val uiState: StateFlow<SettingsUiState> = combine(
        _isLoading,
        _message,
        settingsRepository.isAutoBackupEnabled,
        settingsRepository.backupDirectoryUri,
        settingsRepository.userAge,
        settingsRepository.userSex,
        settingsRepository.userWeight,
        settingsRepository.userHeight,
        settingsRepository.chronicDiseases
    ) { params ->
        val isLoading = params[0] as Boolean
        val message = params[1] as String?
        val isAutoBackupEnabled = params[2] as Boolean
        val backupDirectoryUri = params[3] as String?
        val userAge = params[4] as Int
        val userSex = params[5] as String?
        val userWeight = params[6] as Float
        val userHeight = params[7] as Int
        @Suppress("UNCHECKED_CAST")
        val chronicDiseases = params[8] as Set<String>

        SettingsUiState(
            isLoading = isLoading,
            message = message,
            isAutoBackupEnabled = isAutoBackupEnabled,
            backupDirectoryUri = backupDirectoryUri,
            userAge = userAge,
            userSex = userSex,
            userWeight = userWeight,
            userHeight = userHeight,
            chronicDiseases = chronicDiseases
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState()
    )

    fun exportData(onDataReady: (String) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val json = exportDataUseCase()
                onDataReady(json)
                _message.value = "Export successful"
            } catch (e: Exception) {
                _message.value = "Export failed: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun importData(jsonData: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                importDataUseCase(jsonData)
                _message.value = "Import successful"
            } catch (e: Exception) {
                _message.value = "Import failed: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun toggleAutoBackup(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setAutoBackupEnabled(enabled)
            if (enabled) {
                backupManager.scheduleDailyBackup()
            } else {
                backupManager.cancelDailyBackup()
            }
        }
    }

    fun setBackupDirectory(uri: String?) {
        viewModelScope.launch {
            settingsRepository.setBackupDirectoryUri(uri)
            // Re-schedule to apply new directory (though WorkManager is periodic, it doesn't hurt)
            if (uiState.value.isAutoBackupEnabled) {
                backupManager.scheduleDailyBackup()
            }
        }
    }

    fun resetWelcomeWizard() {
        viewModelScope.launch {
            settingsRepository.setWelcomeWizardCompleted(false)
            _message.value = "Wizard reset. It will appear on next restart."
        }
    }
    
    // Profile Updates
    fun setUserAge(age: Int) { viewModelScope.launch { settingsRepository.setUserAge(age) } }
    fun setUserSex(sex: String?) { viewModelScope.launch { settingsRepository.setUserSex(sex) } }
    fun setUserWeight(weight: Float) { viewModelScope.launch { settingsRepository.setUserWeight(weight) } }
    fun setUserHeight(height: Int) { viewModelScope.launch { settingsRepository.setUserHeight(height) } }
    fun setChronicDiseases(diseases: Set<String>) { viewModelScope.launch { settingsRepository.setChronicDiseases(diseases) } }

    fun clearMessage() {
        _message.value = null
    }
}

data class SettingsUiState(
    val isLoading: Boolean = false,
    val message: String? = null,
    val isAutoBackupEnabled: Boolean = false,
    val backupDirectoryUri: String? = null,
    
    // Profile data
    val userAge: Int = 0,
    val userSex: String? = null,
    val userWeight: Float = 0f,
    val userHeight: Int = 0,
    val chronicDiseases: Set<String> = emptySet()
)
