package org.chronicheal.app.domain.usecase

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.chronicheal.app.data.local.AppDatabase
import org.chronicheal.app.domain.model.BackupData
import org.chronicheal.app.domain.repository.HealthRepository
import javax.inject.Inject

class ExportDataUseCase @Inject constructor(
    private val repository: HealthRepository,
    private val database: AppDatabase,
) {
    private val json = Json { prettyPrint = true }

    suspend operator fun invoke(): String = withContext(Dispatchers.IO) {
        val entries = repository.getAllEntries().first()
        val backupData = BackupData(
            schemaVersion = try {
                database.openHelper.readableDatabase.version
            } catch (_: Exception) {
                0
            },
            entries = entries
        )
        json.encodeToString(backupData)
    }
}
