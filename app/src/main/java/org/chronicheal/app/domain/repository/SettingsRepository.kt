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
}
