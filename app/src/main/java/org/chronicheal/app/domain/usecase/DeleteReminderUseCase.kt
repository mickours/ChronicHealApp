package org.chronicheal.app.domain.usecase

import org.chronicheal.app.data.notification.ReminderScheduler
import org.chronicheal.app.domain.model.Reminder
import org.chronicheal.app.domain.repository.HealthRepository
import javax.inject.Inject

class DeleteReminderUseCase @Inject constructor(
    private val repository: HealthRepository,
    private val reminderScheduler: ReminderScheduler
) {
    suspend operator fun invoke(reminder: Reminder) {
        reminderScheduler.cancel(reminder)
        reminder.templateEntryId?.let { entryId ->
            repository.getEntryById(entryId)?.let { entry ->
                repository.deleteEntry(entry)
            }
        }
        repository.deleteReminder(reminder)
    }
}
