package org.chronicheal.app.domain.repository

import kotlinx.coroutines.flow.Flow
import org.chronicheal.app.domain.model.EntryType

interface SettingsRepository {
    val isWelcomeWizardCompleted: Flow<Boolean>
    suspend fun setWelcomeWizardCompleted(completed: Boolean)

    val favoriteEntryTypes: Flow<Set<EntryType>>
    suspend fun setFavoriteEntryTypes(types: Set<EntryType>)

    val hasShownVoicePermissionRationale: Flow<Boolean>
    suspend fun setHasShownVoicePermissionRationale(shown: Boolean)

    val isAutoBackupEnabled: Flow<Boolean>
    suspend fun setAutoBackupEnabled(enabled: Boolean)

    val backupDirectoryUri: Flow<String?>
    suspend fun setBackupDirectoryUri(uri: String?)

    val isMissingEntryNotificationEnabled: Flow<Boolean>
    suspend fun setMissingEntryNotificationEnabled(enabled: Boolean)

    // User Profile
    val userAge: Flow<Int>
    suspend fun setUserAge(age: Int)

    val userSex: Flow<String?>
    suspend fun setUserSex(sex: String?)

    val userWeight: Flow<Float>
    suspend fun setUserWeight(weight: Float)

    val userHeight: Flow<Int>
    suspend fun setUserHeight(height: Int)

    val chronicDiseases: Flow<Set<String>>
    suspend fun setChronicDiseases(diseases: Set<String>)

    // Allergens
    val allergenOrder: Flow<List<String>>
    suspend fun setAllergenOrder(order: List<String>)

    val deactivatedAllergens: Flow<Set<String>>
    suspend fun setDeactivatedAllergens(allergens: Set<String>)

    // FODMAPs
    val deactivatedFodmaps: Flow<Set<String>>
    suspend fun setDeactivatedFodmaps(fodmaps: Set<String>)
}
