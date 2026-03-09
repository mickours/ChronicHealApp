package org.chronicheal.app.domain.usecase

import kotlinx.serialization.json.Json
import org.chronicheal.app.domain.model.BackupData
import org.chronicheal.app.domain.model.HealthEntry
import org.chronicheal.app.domain.repository.EntryRepository
import javax.inject.Inject

class ImportDataUseCase @Inject constructor(
    private val repository: EntryRepository
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val currentSchemaVersion = 6

    suspend operator fun invoke(jsonData: String) {
        // Try to decode as BackupData first (new format)
        val entries = try {
            val backupData = json.decodeFromString<BackupData>(jsonData)
            
            // Basic version check logic
            if (backupData.schemaVersion > currentSchemaVersion) {
                // Future: implementation actual migration logic if needed
                // For now, we try to import as-is if models are compatible
            }
            
            backupData.entries
        } catch (e: Exception) {
            // Fallback to old format (List<HealthEntry>)
            json.decodeFromString<List<HealthEntry>>(jsonData)
        }
        
        repository.insertEntries(entries)
    }
}
