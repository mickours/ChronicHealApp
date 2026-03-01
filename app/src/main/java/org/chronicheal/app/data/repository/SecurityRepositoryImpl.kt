package org.chronicheal.app.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.chronicheal.app.domain.repository.SecurityRepository
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "security_prefs")

@Singleton
class SecurityRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : SecurityRepository {

    private object PreferencesKeys {
        val BIOMETRIC_LOCK_ENABLED = booleanPreferencesKey("biometric_lock_enabled")
    }

    override val isBiometricLockEnabled: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.BIOMETRIC_LOCK_ENABLED] ?: false
        }

    override suspend fun setBiometricLockEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.BIOMETRIC_LOCK_ENABLED] = enabled
        }
    }
}
