package org.chronicheal.app.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.chronicheal.app.data.ai.LlmManager
import org.chronicheal.app.domain.model.AiMealAnalysis
import org.chronicheal.app.domain.model.EntryType
import org.chronicheal.app.domain.model.HealthEntry
import org.chronicheal.app.domain.model.Reminder
import org.chronicheal.app.domain.repository.HealthRepository
import org.chronicheal.app.domain.repository.SettingsRepository
import org.chronicheal.app.domain.usecase.AddEntryUseCase
import org.chronicheal.app.domain.usecase.AnalyzeMealUseCase
import org.chronicheal.app.domain.usecase.DeleteEntryUseCase
import org.chronicheal.app.domain.usecase.GetEntryByIdUseCase
import org.chronicheal.app.domain.usecase.GetReminderByIdUseCase
import org.chronicheal.app.domain.usecase.GetSuggestionsUseCase
import org.chronicheal.app.domain.usecase.SaveReminderUseCase
import org.chronicheal.app.domain.usecase.UpdateEntryUseCase
import javax.inject.Inject

data class AddEntryUiState(
    val entry: HealthEntry? = null,
    val isNewFromTemplate: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val reminder: Reminder? = null,
    val isAiEnabled: Boolean = true,
    val message: String? = null
)

@HiltViewModel
class AddEntryViewModel @Inject constructor(
    private val addEntryUseCase: AddEntryUseCase,
    private val updateEntryUseCase: UpdateEntryUseCase,
    private val deleteEntryUseCase: DeleteEntryUseCase,
    private val getEntryByIdUseCase: GetEntryByIdUseCase,
    private val getReminderByIdUseCase: GetReminderByIdUseCase,
    private val getSuggestionsUseCase: GetSuggestionsUseCase,
    private val saveReminderUseCase: SaveReminderUseCase,
    private val analyzeMealUseCase: AnalyzeMealUseCase,
    private val healthRepository: HealthRepository,
    private val settingsRepository: SettingsRepository,
    private val llmManager: LlmManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddEntryUiState())
    val uiState: StateFlow<AddEntryUiState> = _uiState.asStateFlow()

    val isDownloadingModel: StateFlow<Boolean> = llmManager.isDownloading
    val modelDownloadProgress: StateFlow<Float> = llmManager.downloadProgress
    val allergenOrder: StateFlow<List<String>> = settingsRepository.allergenOrder
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val deactivatedAllergens: StateFlow<Set<String>> = settingsRepository.deactivatedAllergens
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())
    val deactivatedFodmaps: StateFlow<Set<String>> = settingsRepository.deactivatedFodmaps
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    init {
        viewModelScope.launch {
            _uiState.update { it.copy(isAiEnabled = llmManager.isAiEnabled) }
        }
    }

    fun loadEntry(id: Long?, reminderId: Long?, templateId: Long?) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                when {
                    id != null -> {
                        val entry = getEntryByIdUseCase(id)
                        _uiState.update {
                            it.copy(
                                entry = entry,
                                isNewFromTemplate = false,
                                isLoading = false
                            )
                        }
                    }

                    reminderId != null -> {
                        val entry = healthRepository.getEntryByReminderId(reminderId)
                        val reminder = getReminderByIdUseCase(reminderId)
                        _uiState.update {
                            it.copy(
                                entry = entry,
                                reminder = reminder,
                                isNewFromTemplate = true,
                                isLoading = false
                            )
                        }
                    }

                    templateId != null -> {
                        val entry = getEntryByIdUseCase(templateId)
                        _uiState.update {
                            it.copy(
                                entry = entry,
                                isNewFromTemplate = true,
                                isLoading = false
                            )
                        }
                    }

                    else -> {
                        _uiState.update { it.copy(isLoading = false) }
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun saveEntry(entry: HealthEntry, originalEntry: HealthEntry?) {
        viewModelScope.launch {
            if (originalEntry == null || _uiState.value.isNewFromTemplate) {
                addEntryUseCase(entry)
            } else {
                updateEntryUseCase(entry)
            }
        }
    }

    fun saveEntries(entries: List<HealthEntry>, onComplete: () -> Unit) {
        viewModelScope.launch {
            entries.forEach { addEntryUseCase(it) }
            onComplete()
        }
    }

    fun saveEntryWithReminder(entry: HealthEntry, reminder: Reminder) {
        viewModelScope.launch {
            val entryId = if (entry.id == 0L || _uiState.value.isNewFromTemplate) {
                addEntryUseCase(entry)
            } else {
                updateEntryUseCase(entry)
                entry.id
            }
            saveReminderUseCase(reminder.copy(templateEntryId = entryId))
            updateEntryUseCase(entry.copy(id = entryId, reminderId = reminder.id))
        }
    }

    fun deleteEntry(entry: HealthEntry) {
        viewModelScope.launch {
            deleteEntryUseCase(entry)
        }
    }

    suspend fun getLastEntryByTypeAndName(type: EntryType, name: String): HealthEntry? {
        return healthRepository.getLastEntryByTypeAndName(type, name)
    }

    fun getSuggestions(
        types: Set<EntryType>,
        field: GetSuggestionsUseCase.SuggestionField,
        parentLocation: String? = null
    ): StateFlow<List<String>> {
        return getSuggestionsUseCase.execute(types, field, parentLocation)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    }

    fun isModelPresent(): Boolean = llmManager.isModelPresent()
    fun isMeteredConnection(): Boolean = llmManager.isMeteredConnection()

    fun downloadModel() {
        viewModelScope.launch {
            try {
                llmManager.downloadModel()
                _uiState.update { it.copy(message = "AI model downloaded successfully") }
            } catch (e: Exception) {
                _uiState.update { it.copy(message = "Failed to download: ${e.message}") }
            }
        }
    }

    suspend fun analyzeMeal(description: String): AiMealAnalysis? {
        return analyzeMealUseCase(description)
    }

    suspend fun processLog(text: String): List<HealthEntry>? {
        return llmManager.processLog(text)
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }

    fun showMessage(message: String) {
        _uiState.update { it.copy(message = message) }
    }
}
