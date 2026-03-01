package org.chronicheal.app.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.chronicheal.app.domain.usecase.ExportDataUseCase
import org.chronicheal.app.domain.usecase.ExportPdfUseCase
import org.chronicheal.app.domain.usecase.ImportDataUseCase
import java.io.OutputStream
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val exportDataUseCase: ExportDataUseCase,
    private val importDataUseCase: ImportDataUseCase,
    private val exportPdfUseCase: ExportPdfUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState = _uiState.asStateFlow()

    fun exportData(onDataReady: (String) -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val json = exportDataUseCase()
                onDataReady(json)
                _uiState.value = _uiState.value.copy(message = "Export successful")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(message = "Export failed: ${e.message}")
            } finally {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun importData(jsonData: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                importDataUseCase(jsonData)
                _uiState.value = _uiState.value.copy(message = "Import successful")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(message = "Import failed: ${e.message}")
            } finally {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun exportPdf(outputStream: OutputStream) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                exportPdfUseCase(outputStream)
                _uiState.value = _uiState.value.copy(message = "PDF Export successful")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(message = "PDF Export failed: ${e.message}")
            } finally {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }
    
    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }
}

data class SettingsUiState(
    val isLoading: Boolean = false,
    val message: String? = null
)
