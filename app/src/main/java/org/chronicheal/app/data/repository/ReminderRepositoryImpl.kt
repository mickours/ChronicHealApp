package org.chronicheal.app.data.repository

import kotlinx.coroutines.flow.Flow
import org.chronicheal.app.data.local.ReminderDao
import org.chronicheal.app.domain.model.Reminder
import org.chronicheal.app.domain.repository.ReminderRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReminderRepositoryImpl @Inject constructor(
    private val reminderDao: ReminderDao
) : ReminderRepository {
    override fun getAllReminders(): Flow<List<Reminder>> = reminderDao.getAllReminders()

    override fun getEnabledReminders(): Flow<List<Reminder>> = reminderDao.getEnabledReminders()

    override suspend fun getReminderById(id: Long): Reminder? = reminderDao.getReminderById(id)

    override suspend fun insertReminder(reminder: Reminder): Long = reminderDao.insertReminder(reminder)

    override suspend fun updateReminder(reminder: Reminder) = reminderDao.updateReminder(reminder)

    override suspend fun deleteReminder(reminder: Reminder) = reminderDao.deleteReminder(reminder)
}
