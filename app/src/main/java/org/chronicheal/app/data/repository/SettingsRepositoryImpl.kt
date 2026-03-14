package org.chronicheal.app.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
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
        val IS_AUTO_BACKUP_ENABLED = booleanPreferencesKey("is_auto_backup_enabled")
        val BACKUP_DIRECTORY_URI = stringPreferencesKey("backup_directory_uri")
        
        // Profile
        val USER_AGE = intPreferencesKey("user_age")
        val USER_SEX = stringPreferencesKey("user_sex")
        val USER_WEIGHT = floatPreferencesKey("user_weight")
        val USER_HEIGHT = intPreferencesKey("user_height")
        val CHRONIC_DISEASES = stringSetPreferencesKey("chronic_diseases")
        
        // Allergens
        val ALLERGEN_ORDER = stringPreferencesKey("allergen_order")
        val DEACTIVATED_ALLERGENS = stringSetPreferencesKey("deactivated_allergens")
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
            
            // Removed VOICE_LOGGING from default favorites
            types ?: emptySet()
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

    override val isAutoBackupEnabled: Flow<Boolean> = context.settingsDataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.IS_AUTO_BACKUP_ENABLED] ?: false
        }

    override suspend fun setAutoBackupEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { preferences ->
            preferences[PreferencesKeys.IS_AUTO_BACKUP_ENABLED] = enabled
        }
    }

    override val backupDirectoryUri: Flow<String?> = context.settingsDataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.BACKUP_DIRECTORY_URI]
        }

    override suspend fun setBackupDirectoryUri(uri: String?) {
        context.settingsDataStore.edit { preferences ->
            if (uri == null) {
                preferences.remove(PreferencesKeys.BACKUP_DIRECTORY_URI)
            } else {
                preferences[PreferencesKeys.BACKUP_DIRECTORY_URI] = uri
            }
        }
    }

    // Profile implementation
    override val userAge: Flow<Int> = context.settingsDataStore.data
        .map { preferences -> preferences[PreferencesKeys.USER_AGE] ?: 0 }

    override suspend fun setUserAge(age: Int) {
        context.settingsDataStore.edit { preferences -> preferences[PreferencesKeys.USER_AGE] = age }
    }

    override val userSex: Flow<String?> = context.settingsDataStore.data
        .map { preferences -> preferences[PreferencesKeys.USER_SEX] }

    override suspend fun setUserSex(sex: String?) {
        context.settingsDataStore.edit { preferences ->
            if (sex == null) preferences.remove(PreferencesKeys.USER_SEX)
            else preferences[PreferencesKeys.USER_SEX] = sex
        }
    }

    override val userWeight: Flow<Float> = context.settingsDataStore.data
        .map { preferences -> preferences[PreferencesKeys.USER_WEIGHT] ?: 0f }

    override suspend fun setUserWeight(weight: Float) {
        context.settingsDataStore.edit { preferences -> preferences[PreferencesKeys.USER_WEIGHT] = weight }
    }

    override val userHeight: Flow<Int> = context.settingsDataStore.data
        .map { preferences -> preferences[PreferencesKeys.USER_HEIGHT] ?: 0 }

    override suspend fun setUserHeight(height: Int) {
        context.settingsDataStore.edit { preferences -> preferences[PreferencesKeys.USER_HEIGHT] = height }
    }

    override val chronicDiseases: Flow<Set<String>> = context.settingsDataStore.data
        .map { preferences -> preferences[PreferencesKeys.CHRONIC_DISEASES] ?: emptySet() }

    override suspend fun setChronicDiseases(diseases: Set<String>) {
        context.settingsDataStore.edit { preferences -> preferences[PreferencesKeys.CHRONIC_DISEASES] = diseases }
    }

    // Allergens implementation
    override val allergenOrder: Flow<List<String>> = context.settingsDataStore.data
        .map { preferences ->
            val json = preferences[PreferencesKeys.ALLERGEN_ORDER]
            if (json != null) Json.decodeFromString(json) else emptyList()
        }

    override suspend fun setAllergenOrder(order: List<String>) {
        context.settingsDataStore.edit { preferences ->
            preferences[PreferencesKeys.ALLERGEN_ORDER] = Json.encodeToString(order)
        }
    }

    override val deactivatedAllergens: Flow<Set<String>> = context.settingsDataStore.data
        .map { preferences -> preferences[PreferencesKeys.DEACTIVATED_ALLERGENS] ?: emptySet() }

    override suspend fun setDeactivatedAllergens(allergens: Set<String>) {
        context.settingsDataStore.edit { preferences ->
            preferences[PreferencesKeys.DEACTIVATED_ALLERGENS] = allergens
        }
    }
}
