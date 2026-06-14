package org.chronicheal.app.domain.usecase

import org.chronicheal.app.data.notification.ReminderScheduler
import org.chronicheal.app.domain.model.Reminder
import org.chronicheal.app.domain.repository.EntryRepository
import org.chronicheal.app.domain.repository.ReminderRepository
import javax.inject.Inject

class DeleteReminderUseCase @Inject constructor(
    private val reminderRepository: ReminderRepository,
    private val entryRepository: EntryRepository,
    private val reminderScheduler: ReminderScheduler
) {
    suspend operator fun invoke(reminder: Reminder) {
        reminderScheduler.cancel(reminder)
        reminder.templateEntryId?.let { entryId ->
            entryRepository.getEntryById(entryId)?.let { entry ->
                entryRepository.deleteEntry(entry)
            }
        }
        reminderRepository.deleteReminder(reminder)
    }
}
