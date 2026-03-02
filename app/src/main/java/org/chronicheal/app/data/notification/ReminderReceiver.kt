package org.chronicheal.app.data.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.chronicheal.app.domain.repository.ReminderRepository
import java.time.LocalDate
import javax.inject.Inject

@AndroidEntryPoint
class ReminderReceiver : BroadcastReceiver() {

    @Inject
    lateinit var reminderRepository: ReminderRepository

    @Inject
    lateinit var notificationHelper: NotificationHelper

    @Inject
    lateinit var reminderScheduler: ReminderScheduler

    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getLongExtra("reminder_id", -1L)
        if (reminderId == -1L) return

        when (intent.action) {
            NotificationHelper.ACTION_SKIP -> {
                notificationHelper.cancelNotification(reminderId.toInt())
                return
            }
            NotificationHelper.ACTION_SNOOZE_10 -> {
                notificationHelper.cancelNotification(reminderId.toInt())
                reminderScheduler.snooze(reminderId, 10)
                return
            }
            NotificationHelper.ACTION_SNOOZE_60 -> {
                notificationHelper.cancelNotification(reminderId.toInt())
                reminderScheduler.snooze(reminderId, 60)
                return
            }
        }

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val reminder = reminderRepository.getReminderById(reminderId)
                if (reminder != null && reminder.isEnabled) {
                    val currentDayOfWeek = LocalDate.now().dayOfWeek.value // 1 (Mon) to 7 (Sun)
                    if (reminder.daysOfWeek.contains(currentDayOfWeek)) {
                        notificationHelper.showReminderNotification(
                            title = "Reminder: ${reminder.title}",
                            message = "It's time for your scheduled activity.",
                            reminderId = reminder.id,
                            entryType = reminder.entryType
                        )
                    }
                    // Reschedule for next occurrence
                    reminderScheduler.schedule(reminder)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
