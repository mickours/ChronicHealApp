package org.chronicheal.app.presentation

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.chronicheal.app.domain.repository.SecurityRepository
import javax.inject.Inject

@HiltViewModel
class SecurityViewModel @Inject constructor(
    private val securityRepository: SecurityRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _isBiometricAvailable = MutableStateFlow(false)
    val isBiometricAvailable: StateFlow<Boolean> = _isBiometricAvailable.asStateFlow()

    val isBiometricLockEnabled: StateFlow<Boolean> = securityRepository.isBiometricLockEnabled
        .combine(isBiometricAvailable) { enabled, available ->
            if (!available && enabled) {
                // If enabled but not available (anymore?), we should ideally reset it in repo too
                resetLockIfUnavailable()
                false
            } else {
                enabled && available
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    init {
        checkBiometricAvailability()
    }

    private fun checkBiometricAvailability() {
        val biometricManager = BiometricManager.from(context)
        val canAuthenticate = biometricManager.canAuthenticate(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)
        _isBiometricAvailable.value = canAuthenticate == BiometricManager.BIOMETRIC_SUCCESS
    }

    private fun resetLockIfUnavailable() {
        viewModelScope.launch {
            securityRepository.setBiometricLockEnabled(false)
        }
    }

    fun setBiometricLockEnabled(enabled: Boolean) {
        if (_isBiometricAvailable.value) {
            viewModelScope.launch {
                securityRepository.setBiometricLockEnabled(enabled)
            }
        }
    }
}
