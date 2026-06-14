package org.chronicheal.app.data.repository

import kotlinx.coroutines.flow.Flow
import org.chronicheal.app.data.local.EntryDao
import org.chronicheal.app.data.local.ReminderDao
import org.chronicheal.app.domain.model.EntryType
import org.chronicheal.app.domain.model.HealthEntry
import org.chronicheal.app.domain.model.Reminder
import org.chronicheal.app.domain.repository.HealthRepository
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HealthRepositoryImpl @Inject constructor(
    private val entryDao: EntryDao,
    private val reminderDao: ReminderDao
) : HealthRepository {

    // Entries
    override fun getAllEntries(): Flow<List<HealthEntry>> = entryDao.getAllEntries()
    
    override suspend fun getEntryById(id: Long): HealthEntry? = entryDao.getEntryById(id)
    
    override suspend fun getEntryByReminderId(reminderId: Long): HealthEntry? = 
        entryDao.getEntryByReminderId(reminderId)
    
    override suspend fun getLastEntryByTypeAndName(type: EntryType, name: String): HealthEntry? =
        entryDao.getLastEntryByTypeAndName(type, name)
    
    override suspend fun getEntriesSince(since: Instant): List<HealthEntry> =
        entryDao.getEntriesSince(since)
    
    override suspend fun insertEntry(entry: HealthEntry): Long = entryDao.insertEntry(entry)
    
    override suspend fun insertEntries(entries: List<HealthEntry>) = entryDao.insertEntries(entries)
    
    override suspend fun updateEntry(entry: HealthEntry) = entryDao.updateEntry(entry)
    
    override suspend fun deleteEntry(entry: HealthEntry) = entryDao.deleteEntry(entry)

    // Reminders
    override fun getAllReminders(): Flow<List<Reminder>> = reminderDao.getAllReminders()

    override fun getEnabledReminders(): Flow<List<Reminder>> = reminderDao.getEnabledReminders()

    override suspend fun getReminderById(id: Long): Reminder? = reminderDao.getReminderById(id)

    override suspend fun getRemindersByTypeAndName(type: EntryType?, name: String): List<Reminder> =
        type?.let { reminderDao.getRemindersByTypeAndName(it, name) } ?: emptyList()

    override suspend fun insertReminder(reminder: Reminder): Long = reminderDao.insertReminder(reminder)

    override suspend fun updateReminder(reminder: Reminder) = reminderDao.updateReminder(reminder)

    override suspend fun deleteReminder(reminder: Reminder) = reminderDao.deleteReminder(reminder)
}
