package org.chronicheal.app.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.chronicheal.app.domain.model.EntryType
import org.chronicheal.app.domain.repository.SettingsRepository
import javax.inject.Inject
import javax.inject.Singleton

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings_prefs")

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : SettingsRepository {

    private object PreferencesKeys {
        val WELCOME_WIZARD_COMPLETED = booleanPreferencesKey("welcome_wizard_completed")
        val FAVORITE_ENTRY_TYPES = stringSetPreferencesKey("favorite_entry_types")
        val SHOWN_VOICE_PERMISSION_RATIONALE = booleanPreferencesKey("shown_voice_permission_rationale")
    }

    override val isWelcomeWizardCompleted: Flow<Boolean> = context.settingsDataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.WELCOME_WIZARD_COMPLETED] ?: false
        }

    override suspend fun setWelcomeWizardCompleted(completed: Boolean) {
        context.settingsDataStore.edit { preferences ->
            preferences[PreferencesKeys.WELCOME_WIZARD_COMPLETED] = completed
        }
    }

    override val favoriteEntryTypes: Flow<Set<EntryType>> = context.settingsDataStore.data
        .map { preferences ->
            val types = preferences[PreferencesKeys.FAVORITE_ENTRY_TYPES]?.mapNotNull {
                try {
                    EntryType.valueOf(it)
                } catch (e: IllegalArgumentException) {
                    null
                }
            }?.toSet()
            
            types ?: setOf(EntryType.VOICE_LOGGING) // Default to Voice Logging if not set
        }

    override suspend fun setFavoriteEntryTypes(types: Set<EntryType>) {
        context.settingsDataStore.edit { preferences ->
            preferences[PreferencesKeys.FAVORITE_ENTRY_TYPES] = types.map { it.name }.toSet()
        }
    }

    override val hasShownVoicePermissionRationale: Flow<Boolean> = context.settingsDataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.SHOWN_VOICE_PERMISSION_RATIONALE] ?: false
        }

    override suspend fun setHasShownVoicePermissionRationale(shown: Boolean) {
        context.settingsDataStore.edit { preferences ->
            preferences[PreferencesKeys.SHOWN_VOICE_PERMISSION_RATIONALE] = shown
        }
    }
}
