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
import org.chronicheal.app.domain.repository.HealthRepository
import org.chronicheal.app.domain.repository.SettingsRepository
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.math.sqrt

@HiltWorker
class MissingEntryWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val healthRepository: HealthRepository,
    private val settingsRepository: SettingsRepository,
    private val notificationHelper: NotificationHelper
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val MAX_NOTIFICATIONS_PER_DAY = 2
        private val QUIET_HOURS_START = LocalTime.of(21, 30)
        private val QUIET_HOURS_END = LocalTime.of(8, 30)
        private const val RECENT_LOG_GRACE_PERIOD_MINS = 60L
    }

    override suspend fun doWork(): Result {
        // 1. Check if missing entry notifications are enabled in settings
        val isEnabled = settingsRepository.isMissingEntryNotificationEnabled.first()
        if (!isEnabled) {
            return Result.success()
        }

        val now = Instant.now()
        val zoneId = ZoneId.systemDefault()
        val localDateTime = now.atZone(zoneId)
        val localTimeNow = localDateTime.toLocalTime()
        val todayStr = localDateTime.toLocalDate().toString()

        // 2. Quiet Hours check: Don't disturb the user early morning or late night
        if (localTimeNow.isAfter(QUIET_HOURS_START) || localTimeNow.isBefore(QUIET_HOURS_END)) {
            return Result.success()
        }

        // 3. Activity check: If user recently logged anything, they are active in the app, no need to nudge
        val proximityThreshold = now.minus(RECENT_LOG_GRACE_PERIOD_MINS, ChronoUnit.MINUTES)
        val recentEntries = healthRepository.getEntriesSince(proximityThreshold)
        if (recentEntries.isNotEmpty()) {
            return Result.success()
        }

        // 4. Daily Cap check: Limit total nudges per day to avoid being annoying
        val lastNotifDates = settingsRepository.lastMissingEntryNotificationDates.first()
        val notificationsSentToday = lastNotifDates.values.count { it == todayStr }
        if (notificationsSentToday >= MAX_NOTIFICATIONS_PER_DAY) {
            return Result.success()
        }

        // 5. Fetch past entries to identify habits (2-week window)
        val twoWeeksAgo = now.minus(14, ChronoUnit.DAYS)
        val threeDaysAgo = now.minus(3, ChronoUnit.DAYS)
        val pastEntries = healthRepository.getEntriesSince(twoWeeksAgo)

        // 6. Identify consistent and ACTIVE habits
        // We focus on types that usually have names and occur at semi-regular times
        val targetTypes = setOf(
            EntryType.MEAL,
            EntryType.STOOL,
            EntryType.DRUG,
            EntryType.BEVERAGE,
            EntryType.ACTIVITY
        )

        val regularPatterns = pastEntries
            .filter { it.type in targetTypes && !it.name.isNullOrBlank() }
            .groupBy { it.type to it.name!! }
            .filter { (_, entries) ->
                // Threshold 1: Frequent enough (at least 9 times in 14 days)
                val isFrequent = entries.size >= 9
                // Threshold 2: Active habit (must have been logged at least once in the last 3 days)
                val isStillActive = entries.any { it.timestamp.isAfter(threeDaysAgo) }
                isFrequent && isStillActive
            }
            .mapNotNull { (key, entries) ->
                val timesInSeconds = entries.map {
                    it.timestamp.atZone(zoneId).toLocalTime().toSecondOfDay().toDouble()
                }
                val avgSeconds = timesInSeconds.average()
                val variance =
                    timesInSeconds.map { (it - avgSeconds) * (it - avgSeconds) }.average()
                val stdDevMinutes = sqrt(variance) / 60.0

                // Threshold 3: Highly regular (stddev < 40 mins)
                if (stdDevMinutes > 40.0) return@mapNotNull null

                key to LocalTime.ofSecondOfDay(avgSeconds.toLong())
            }.toMap()

        // 7. Check what's missing today and pick the best one to notify
        val todayStart = localDateTime.toLocalDate().atStartOfDay(zoneId).toInstant()
        val entriesToday = healthRepository.getEntriesSince(todayStart)

        val validatedMissing = mutableListOf<Triple<EntryType, String, LocalTime>>()
        for ((key, typicalTime) in regularPatterns) {
            val (type, name) = key
            val patternKey = type?.let { "${it.name}_$name" }

            // A. Not yet notified today for this specific item?
            if (lastNotifDates[patternKey] == todayStr) continue

            // B. No active manual reminder? (Avoid double notification if user already set a reminder)
            val existingReminders = healthRepository.getRemindersByTypeAndName(type, name)
            if (existingReminders.any { it.isEnabled }) continue

            // C. Notification Window: typicalTime + (2h to 6h)
            // Giving a 2h grace period before nudging to allow for slight daily variations
            val gracePeriodStart = typicalTime.plusHours(2)
            val gracePeriodEnd = typicalTime.plusHours(6)

            if (localTimeNow.isAfter(gracePeriodStart) && localTimeNow.isBefore(gracePeriodEnd)) {
                // D. Not already logged today?
                val alreadyLogged = entriesToday.any { it.type == type && it.name == name }
                if (!alreadyLogged) {
                    validatedMissing.add(
                        Triple(
                            type,
                            name,
                            typicalTime
                        ) as Triple<EntryType, String, LocalTime>
                    )
                }
            }
        }

        // 8. If multiple missing, prioritize by type (Drugs first) and then by how overdue they are
        val itemToNotify = validatedMissing
            .sortedWith(compareByDescending<Triple<EntryType, String, LocalTime>> { it.first == EntryType.DRUG }
                .thenBy { it.third }) // Earliest typical time first (most overdue)
            .firstOrNull()

        if (itemToNotify != null) {
            val (type, name, _) = itemToNotify
            val notificationId = (type.name + name).hashCode()
            val lastEntry = healthRepository.getLastEntryByTypeAndName(type, name)

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

            // Record that we notified for this pattern today
            val patternKey = "${type.name}_$name"
            settingsRepository.setLastMissingEntryNotificationDate(patternKey, todayStr)
        }

        return Result.success()
    }
}
