package org.chronicheal.app.domain.usecase

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import org.chronicheal.app.data.local.AppDatabase
import org.chronicheal.app.domain.model.BackupData
import org.chronicheal.app.domain.model.HealthEntry
import org.chronicheal.app.domain.repository.HealthRepository
import javax.inject.Inject

class ImportDataUseCase @Inject constructor(
    private val repository: HealthRepository,
    private val database: AppDatabase,
) {
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
    }

    suspend operator fun invoke(jsonData: String) = withContext(Dispatchers.IO) {
        // Handle UTF-8 BOM if present
        val cleanData = jsonData.removePrefix("\uFEFF").trimStart()

        if (cleanData.isBlank()) {
            throw IllegalArgumentException("Import failed: File is empty")
        }

        val entries = try {
            if (cleanData.startsWith("{")) {
                // New format: BackupData object
                val backupData = try {
                    json.decodeFromString<BackupData>(cleanData)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to parse BackupData", e)
                    throw IllegalArgumentException("Failed to parse backup file: ${e.localizedMessage}")
                }

                val currentVersion = getCurrentDatabaseVersion()
                if (backupData.schemaVersion > currentVersion && currentVersion > 0) {
                    Log.w(
                        TAG,
                        "Backup version (${backupData.schemaVersion}) is newer than app version ($currentVersion)"
                    )
                    throw VersionMismatchException(
                        "This backup was created with a newer version of the app (${backupData.schemaVersion}). " +
                                "Please update ChronicHeal to the latest version to import this data."
                    )
                }
                backupData.entries
            } else if (cleanData.startsWith("[")) {
                // Old format: List of entries
                try {
                    json.decodeFromString<List<HealthEntry>>(cleanData)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to parse legacy entry list", e)
                    throw IllegalArgumentException("Failed to parse legacy entry list: ${e.localizedMessage}")
                }
            } else {
                throw IllegalArgumentException("Invalid data format: Expected a JSON object or array.")
            }
        } catch (e: VersionMismatchException) {
            throw e
        } catch (e: IllegalArgumentException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected import error", e)
            throw IllegalArgumentException("Import failed due to an unexpected error: ${e.localizedMessage}")
        }

        if (entries.isEmpty()) {
            throw IllegalArgumentException("Import failed: No health entries found in the file")
        }

        try {
            repository.insertEntries(entries)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to insert entries into database", e)
            throw IllegalArgumentException("Failed to save imported data to database: ${e.localizedMessage}")
        }
    }

    private fun getCurrentDatabaseVersion(): Int = try {
        database.openHelper.readableDatabase.version
    } catch (e: Exception) {
        Log.e(TAG, "Failed to get database version", e)
        0
    }

    companion object {
        private const val TAG = "ImportDataUseCase"
    }

    /**
     * Exception thrown when the backup file is from a newer version of the app than the current one.
     */
    class VersionMismatchException(message: String) : Exception(message)
}
