package org.chronicheal.app.domain.usecase

import org.chronicheal.app.data.notification.ReminderScheduler
import org.chronicheal.app.domain.model.Reminder
import org.chronicheal.app.domain.repository.HealthRepository
import javax.inject.Inject

class ToggleReminderUseCase @Inject constructor(
    private val repository: HealthRepository,
    private val scheduler: ReminderScheduler
) {
    suspend operator fun invoke(reminder: Reminder) {
        val updated = reminder.copy(isEnabled = !reminder.isEnabled)
        repository.updateReminder(updated)
        if (updated.isEnabled) {
            scheduler.schedule(updated)
        } else {
            scheduler.cancel(updated)
        }
    }
}
