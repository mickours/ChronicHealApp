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
        if (jsonData.isBlank()) {
            throw IllegalArgumentException("Import failed: File is empty")
        }

        val currentVersion = getCurrentDatabaseVersion()

        // Try to decode as BackupData first (new format)
        val entries = try {
            val backupData = json.decodeFromString<BackupData>(jsonData)

            if (backupData.schemaVersion > currentVersion && currentVersion > 0) {
                Log.w(
                    TAG,
                    "Backup version (${backupData.schemaVersion}) is newer than app version ($currentVersion)"
                )
                throw VersionMismatchException(
                    "This backup was created with a newer version of the app. " +
                            "Please update ChronicHeal to the latest version to import this data."
                )
            }

            backupData.entries
        } catch (e: VersionMismatchException) {
            // Re-throw version mismatch so it's not swallowed by the general fallback catch
            throw e
        } catch (_: Exception) {
            // Fallback to old format (List<HealthEntry>)
            try {
                json.decodeFromString<List<HealthEntry>>(jsonData)
            } catch (_: Exception) {
                // If both fail, throw a more descriptive error
                throw IllegalArgumentException("Invalid data format: This file does not appear to be a valid ChronicHeal backup.")
            }
        }

        if (entries.isEmpty()) {
            throw IllegalArgumentException("Import failed: No health entries found in the file")
        }

        repository.insertEntries(entries)
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
