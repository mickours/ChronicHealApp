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
        const val ACTION_SKIP = "org.chronicheal.app.ACTION_SKIP"
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
                description = "Channel for health reminders"
                enableLights(true)
                enableVibration(true)
                setShowBadge(true)
                setLockscreenVisibility(android.app.Notification.VISIBILITY_PUBLIC)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showReminderNotification(title: String, message: String, reminderId: Long, entryType: EntryType? = null) {
        val openIntent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_MAIN
            addCategory(Intent.CATEGORY_LAUNCHER)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            if (entryType != null) {
                putExtra(EXTRA_ENTRY_TYPE, entryType.name)
            }
        }
        
        val openPendingIntent = PendingIntent.getActivity(
            context,
            reminderId.toInt(),
            openIntent,
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

        // Using a more standard system icon for compatibility
        val builder = NotificationCompat.Builder(context, REMINDER_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(openPendingIntent)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        if (entryType == EntryType.PAIN) {
            builder.addAction(
                android.R.drawable.ic_menu_edit,
                "Start Body Scan",
                openPendingIntent
            )
            builder.addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Skip Today",
                skipPendingIntent
            )
        }

        notificationManager.notify(reminderId.toInt(), builder.build())
    }

    fun cancelNotification(notificationId: Int) {
        notificationManager.cancel(notificationId)
    }
}
