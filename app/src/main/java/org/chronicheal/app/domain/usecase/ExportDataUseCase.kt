package org.chronicheal.app.domain.usecase

import kotlinx.coroutines.flow.first
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.chronicheal.app.domain.model.BackupData
import org.chronicheal.app.domain.repository.EntryRepository
import javax.inject.Inject

class ExportDataUseCase @Inject constructor(
    private val repository: EntryRepository
) {
    private val json = Json { prettyPrint = true }
    private val currentSchemaVersion = 6 // Current version in AppDatabase

    suspend operator fun invoke(): String {
        val entries = repository.getAllEntries().first()
        val backupData = BackupData(
            schemaVersion = currentSchemaVersion,
            entries = entries
        )
        return json.encodeToString(backupData)
    }
}
