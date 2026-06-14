package org.chronicheal.app.data.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.chronicheal.app.domain.repository.HealthRepository
import javax.inject.Inject

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject
    lateinit var healthRepository: HealthRepository

    @Inject
    lateinit var reminderScheduler: ReminderScheduler

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val enabledReminders = healthRepository.getEnabledReminders().first()
                    enabledReminders.forEach { reminder ->
                        reminderScheduler.schedule(reminder)
                    }
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
