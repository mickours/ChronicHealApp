package org.chronicheal.app.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import org.chronicheal.app.data.notification.NotificationHelper
import org.chronicheal.app.domain.model.EntryType
import org.chronicheal.app.domain.repository.EntryRepository
import org.chronicheal.app.domain.repository.SettingsRepository
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit

@HiltWorker
class MissingEntryWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val entryRepository: EntryRepository,
    private val settingsRepository: SettingsRepository,
    private val notificationHelper: NotificationHelper
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        // Check if missing entry notifications are enabled in settings
        val isEnabled = settingsRepository.isMissingEntryNotificationEnabled.first()
        if (!isEnabled) {
            return Result.success()
        }

        val now = Instant.now()
        val zoneId = ZoneId.systemDefault()
        val localTimeNow = now.atZone(zoneId).toLocalTime()
        val localDateNow = now.atZone(zoneId).toLocalDate()

        // 1. Get entries from the last 14 days to identify patterns
        val twoWeeksAgo = now.minus(14, ChronoUnit.DAYS)
        val pastEntries = entryRepository.getEntriesSince(twoWeeksAgo)

        // 2. Group entries by Type and Name to find regular ones
        // We only care about MEAL, STOOL, DRUG, BEVERAGE as requested
        val targetTypes = setOf(EntryType.MEAL, EntryType.STOOL, EntryType.DRUG, EntryType.BEVERAGE)

        val regularPatterns = pastEntries
            .filter { it.type in targetTypes && !it.name.isNullOrBlank() }
            .groupBy { it.type to it.name!! }
            .filter { (_, entries) -> entries.size >= 4 } // Occurred at least 4 times in 14 days
            .mapValues { (_, entries) ->
                val times = entries.map { it.timestamp.atZone(zoneId).toLocalTime() }
                val avgSeconds = times.map { it.toSecondOfDay() }.average().toInt()
                LocalTime.ofSecondOfDay(avgSeconds.toLong())
            }

        // 3. Check what's missing today
        val todayStart = localDateNow.atStartOfDay(zoneId).toInstant()
        val entriesToday = entryRepository.getEntriesSince(todayStart)

        for (entry in regularPatterns) {
            val key = entry.key
            val type = key.first
            val name = key.second
            val typicalTime = entry.value

            // If the typical time has passed (plus a grace period, e.g., 1 hour)
            val gracePeriodEnd = typicalTime.plusHours(1)

            if (localTimeNow.isAfter(gracePeriodEnd)) {
                // Check if this specific entry exists today
                val alreadyLogged = entriesToday.any { it.type == type && it.name == name }

                if (!alreadyLogged) {
                    // It's missing! Show notification.
                    // We use a unique ID based on type and name hash to avoid duplicate notifications for the same thing
                    val notificationId = (type.name + name).hashCode()

                    val lastEntry = entryRepository.getLastEntryByTypeAndName(type, name)

                    notificationHelper.showMissingEntryNotification(
                        title = context.getString(org.chronicheal.app.R.string.notification_missing_entry_title),
                        message = context.getString(
                            org.chronicheal.app.R.string.notification_missing_entry_message,
                            name
                        ),
                        notificationId = notificationId,
                        entryType = type,
                        templateEntryId = lastEntry?.id
                    )
                }
            }
        }

        return Result.success()
    }
}
