package org.chronicheal.app.data.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import org.chronicheal.app.MainActivity
import org.chronicheal.app.R
import org.chronicheal.app.domain.model.EntryType
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    companion object {
        const val REMINDER_CHANNEL_ID = "reminder_channel"
        const val REMINDER_CHANNEL_NAME = "Reminders"
        
        const val EXTRA_ENTRY_TYPE = "extra_entry_type"
        const val EXTRA_REMINDER_ID = "extra_reminder_id"
        const val EXTRA_TEMPLATE_ENTRY_ID = "extra_template_entry_id"
        const val EXTRA_IS_LOG_NOW = "extra_is_log_now"
        
        const val ACTION_SKIP = "org.chronicheal.app.ACTION_SKIP"
        const val ACTION_LOG_NOW = "org.chronicheal.app.ACTION_LOG_NOW"
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                REMINDER_CHANNEL_ID,
                REMINDER_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.checkup_reminder_desc)
                enableLights(true)
                enableVibration(true)
                setShowBadge(true)
                setLockscreenVisibility(android.app.Notification.VISIBILITY_PUBLIC)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showReminderNotification(title: String, message: String, reminderId: Long, entryType: EntryType? = null) {
        // Use a consistent intent for both main click and Log Now action, distinguishing with isLogNow extra
        fun createActivityIntent(isLogNow: Boolean): Intent {
            return Intent(context, MainActivity::class.java).apply {
                action = Intent.ACTION_MAIN
                addCategory(Intent.CATEGORY_LAUNCHER)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                if (entryType != null) {
                    putExtra(EXTRA_ENTRY_TYPE, entryType.name)
                }
                putExtra(EXTRA_REMINDER_ID, reminderId)
                putExtra(EXTRA_IS_LOG_NOW, isLogNow)
            }
        }
        
        val openPendingIntent = PendingIntent.getActivity(
            context,
            reminderId.toInt(),
            createActivityIntent(isLogNow = false),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val logNowPendingIntent = PendingIntent.getActivity(
            context,
            reminderId.toInt() + 4000,
            createActivityIntent(isLogNow = true),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val skipIntent = Intent(context, ReminderReceiver::class.java).apply {
            action = ACTION_SKIP
            putExtra("reminder_id", reminderId)
        }
        
        val skipPendingIntent = PendingIntent.getBroadcast(
            context,
            reminderId.toInt() + 1000,
            skipIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(context, REMINDER_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_logo)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(openPendingIntent)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(
                android.R.drawable.ic_menu_add, 
                context.getString(R.string.notification_action_log_now), 
                logNowPendingIntent
            )
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                context.getString(R.string.notification_action_skip),
                skipPendingIntent
            )

        notificationManager.notify(reminderId.toInt(), builder.build())
    }

    fun showMissingEntryNotification(
        title: String,
        message: String,
        notificationId: Int,
        entryType: EntryType,
        templateEntryId: Long?
    ) {
        fun createActivityIntent(): Intent {
            return Intent(context, MainActivity::class.java).apply {
                action = Intent.ACTION_MAIN
                addCategory(Intent.CATEGORY_LAUNCHER)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra(EXTRA_ENTRY_TYPE, entryType.name)
                if (templateEntryId != null) {
                    putExtra(EXTRA_TEMPLATE_ENTRY_ID, templateEntryId)
                }
                putExtra(EXTRA_IS_LOG_NOW, true)
            }
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            createActivityIntent(),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(context, REMINDER_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_logo)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(
                android.R.drawable.ic_menu_add,
                context.getString(R.string.notification_action_log_now),
                pendingIntent
            )

        notificationManager.notify(notificationId, builder.build())
    }

    fun cancelNotification(notificationId: Int) {
        notificationManager.cancel(notificationId)
    }
}
