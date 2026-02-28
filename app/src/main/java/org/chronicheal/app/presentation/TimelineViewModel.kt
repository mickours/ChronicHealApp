package org.chronicheal.app.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.chronicheal.app.domain.model.HealthEntry
import org.chronicheal.app.domain.usecase.AddEntryUseCase
import org.chronicheal.app.domain.usecase.GetEntriesUseCase
import javax.inject.Inject

@HiltViewModel
class TimelineViewModel @Inject constructor(
    private val getEntriesUseCase: GetEntriesUseCase,
    private val addEntryUseCase: AddEntryUseCase
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
}

data class TimelineUiState(
    val entries: List<HealthEntry> = emptyList(),
    val isLoading: Boolean = false
)
