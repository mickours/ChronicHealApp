package org.chronicheal.app.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.chronicheal.app.domain.repository.SecurityRepository
import javax.inject.Inject

@HiltViewModel
class SecurityViewModel @Inject constructor(
    private val securityRepository: SecurityRepository
) : ViewModel() {

    val isBiometricLockEnabled: StateFlow<Boolean> = securityRepository.isBiometricLockEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun setBiometricLockEnabled(enabled: Boolean) {
        viewModelScope.launch {
            securityRepository.setBiometricLockEnabled(enabled)
        }
    }
}
