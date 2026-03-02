package org.chronicheal.app.domain.repository

import kotlinx.coroutines.flow.Flow
import org.chronicheal.app.domain.model.EntryType

interface SettingsRepository {
    val isWelcomeWizardCompleted: Flow<Boolean>
    suspend fun setWelcomeWizardCompleted(completed: Boolean)

    val favoriteEntryTypes: Flow<Set<EntryType>>
    suspend fun setFavoriteEntryTypes(types: Set<EntryType>)
}
