package org.chronicheal.app.data.worker

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import org.chronicheal.app.domain.repository.SettingsRepository
import org.chronicheal.app.domain.usecase.ExportDataUseCase
import java.io.File
import java.time.LocalDate

@HiltWorker
class BackupWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val exportDataUseCase: ExportDataUseCase,
    private val settingsRepository: SettingsRepository
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            Log.d(TAG, "Starting automated backup...")
            val jsonData = exportDataUseCase()
            val today = LocalDate.now()
            val fileName = "${FILE_PREFIX}${today}.json"

            val customUriString = settingsRepository.backupDirectoryUri.first()
            if (customUriString != null) {
                Log.d(TAG, "Saving to external storage: $customUriString")
                val directoryUri = Uri.parse(customUriString)
                saveToExternalStorage(directoryUri, fileName, jsonData)
            } else {
                Log.d(TAG, "Saving to internal storage")
                saveToInternalStorage(fileName, jsonData)
            }

            Log.d(TAG, "Backup completed successfully")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Backup failed", e)
            Result.retry()
        }
    }

    private fun saveToInternalStorage(fileName: String, content: String) {
        val backupDir = File(context.filesDir, "backups")
        if (!backupDir.exists()) {
            backupDir.mkdirs()
        }
        val backupFile = File(backupDir, fileName)
        backupFile.writeText(content)
        performInternalRollingCleanup(backupDir)
    }

    private fun saveToExternalStorage(directoryUri: Uri, fileName: String, content: String) {
        val root = DocumentFile.fromTreeUri(context, directoryUri)
        if (root == null) {
            Log.e(TAG, "Failed to get root document from URI: $directoryUri")
            return
        }
        
        // Find or create the file
        var file = root.findFile(fileName)
        if (file == null) {
            file = root.createFile("application/json", fileName)
        }

        if (file == null) {
            Log.e(TAG, "Failed to create/find backup file in external storage")
            return
        }

        context.contentResolver.openOutputStream(file.uri)?.use { outputStream ->
            outputStream.write(content.toByteArray())
        } ?: Log.e(TAG, "Failed to open output stream for external backup file")
        
        performExternalRollingCleanup(root)
    }

    private fun performInternalRollingCleanup(backupDir: File) {
        val files = backupDir.listFiles { file ->
            (file.name.startsWith(FILE_PREFIX) || file.name.startsWith("backup_")) && file.name.endsWith(
                ".json"
            )
        } ?: return

        val today = LocalDate.now()
        val sevenDaysAgo = today.minusDays(7)

        val monthlyFiles = files.groupBy { file ->
            try {
                val dateStr = file.name.removePrefix(FILE_PREFIX).removePrefix("backup_")
                    .removeSuffix(".json")
                val date = LocalDate.parse(dateStr)
                "${date.year}-${date.monthValue}"
            } catch (e: Exception) {
                "unknown"
            }
        }

        files.forEach { file ->
            try {
                val dateStr = file.name.removePrefix(FILE_PREFIX).removePrefix("backup_")
                    .removeSuffix(".json")
                val date = LocalDate.parse(dateStr)
                val isRecent = !date.isBefore(sevenDaysAgo)
                val monthKey = "${date.year}-${date.monthValue}"
                val isFirstOfMonth =
                    monthlyFiles[monthKey]?.sortedBy { it.name }?.firstOrNull() == file

                if (!isRecent && !isFirstOfMonth) {
                    file.delete()
                }
            } catch (e: Exception) {
                // Skip files with invalid naming
            }
        }
    }

    private fun performExternalRollingCleanup(root: DocumentFile) {
        val files = root.listFiles().filter {
            (it.name?.startsWith(FILE_PREFIX) == true || it.name?.startsWith("backup_") == true) && it.name?.endsWith(
                ".json"
            ) == true
        }
        if (files.isEmpty()) return

        val today = LocalDate.now()
        val sevenDaysAgo = today.minusDays(7)

        val monthlyFiles = files.groupBy { file ->
            val name = file.name ?: ""
            val dateStr =
                name.removePrefix(FILE_PREFIX).removePrefix("backup_").removeSuffix(".json")
            try {
                val date = LocalDate.parse(dateStr)
                "${date.year}-${date.monthValue}"
            } catch (e: Exception) {
                "unknown"
            }
        }

        files.forEach { file ->
            val name = file.name ?: return@forEach
            try {
                val dateStr =
                    name.removePrefix(FILE_PREFIX).removePrefix("backup_").removeSuffix(".json")
                val date = LocalDate.parse(dateStr)
                
                val isRecent = !date.isBefore(sevenDaysAgo)
                val monthKey = "${date.year}-${date.monthValue}"
                val isFirstOfMonth = monthlyFiles[monthKey]?.sortedBy { it.name }?.firstOrNull() == file

                if (!isRecent && !isFirstOfMonth) {
                    file.delete()
                }
            } catch (e: Exception) {
                // Ignore parsing errors for custom files
            }
        }
    }

    companion object {
        private const val TAG = "BackupWorker"
        private const val FILE_PREFIX = "chronicheal_backup_"
    }
}
