package org.chronicheal.app.domain.usecase

import org.chronicheal.app.data.notification.ReminderScheduler
import org.chronicheal.app.domain.model.HealthEntry
import org.chronicheal.app.domain.model.Reminder
import org.chronicheal.app.domain.repository.EntryRepository
import org.chronicheal.app.domain.repository.ReminderRepository
import javax.inject.Inject

class SaveReminderUseCase @Inject constructor(
    private val reminderRepository: ReminderRepository,
    private val entryRepository: EntryRepository,
    private val reminderScheduler: ReminderScheduler
) {
    suspend operator fun invoke(reminder: Reminder, templateEntry: HealthEntry? = null): Long {
        var templateId = reminder.templateEntryId

        if (templateEntry != null) {
            if (templateId != null && templateId != 0L) {
                entryRepository.updateEntry(templateEntry.copy(id = templateId))
            } else {
                templateId = entryRepository.insertEntry(templateEntry)
            }
        }

        val reminderToSave = reminder.copy(templateEntryId = templateId)
        val id = if (reminderToSave.id == 0L) {
            reminderRepository.insertReminder(reminderToSave)
        } else {
            reminderRepository.updateReminder(reminderToSave)
            reminderToSave.id
        }

        val finalReminder = reminderToSave.copy(id = id)
        if (finalReminder.isEnabled) {
            reminderScheduler.schedule(finalReminder)
        } else {
            reminderScheduler.cancel(finalReminder)
        }

        return id
    }
}
