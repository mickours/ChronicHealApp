package org.chronicheal.app.domain.usecase

import kotlinx.serialization.json.Json
import org.chronicheal.app.data.local.AppDatabase
import org.chronicheal.app.domain.model.BackupData
import org.chronicheal.app.domain.model.HealthEntry
import org.chronicheal.app.domain.repository.HealthRepository
import javax.inject.Inject

class ImportDataUseCase @Inject constructor(
    private val repository: HealthRepository,
    private val database: AppDatabase
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend operator fun invoke(jsonData: String) {
        val currentVersion = database.openHelper.readableDatabase.version
        
        // Try to decode as BackupData first (new format)
        val entries = try {
            val backupData = json.decodeFromString<BackupData>(jsonData)

            if (backupData.schemaVersion > currentVersion) {
                // Future: handle cases where backup is from a newer app version
            }
            
            backupData.entries
        } catch (e: Exception) {
            // Fallback to old format (List<HealthEntry>)
            json.decodeFromString<List<HealthEntry>>(jsonData)
        }
        
        repository.insertEntries(entries)
    }
}
