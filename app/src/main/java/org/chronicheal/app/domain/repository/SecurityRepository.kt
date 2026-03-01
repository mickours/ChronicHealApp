package org.chronicheal.app.domain.repository

import kotlinx.coroutines.flow.Flow

interface SecurityRepository {
    val isBiometricLockEnabled: Flow<Boolean>
    suspend fun setBiometricLockEnabled(enabled: Boolean)
}
