package org.chronicheal.app.domain.repository

import kotlinx.coroutines.flow.Flow
import org.chronicheal.app.domain.model.EntryType
import org.chronicheal.app.domain.model.HealthEntry
import org.chronicheal.app.domain.model.Reminder
import java.time.Instant

interface HealthRepository {
    // Entries
    fun getAllEntries(): Flow<List<HealthEntry>>
    suspend fun getEntryById(id: Long): HealthEntry?
    suspend fun getEntryByReminderId(reminderId: Long): HealthEntry?
    suspend fun getLastEntryByTypeAndName(type: EntryType, name: String): HealthEntry?
    suspend fun getEntriesSince(since: Instant): List<HealthEntry>
    suspend fun insertEntry(entry: HealthEntry): Long
    suspend fun insertEntries(entries: List<HealthEntry>)
    suspend fun updateEntry(entry: HealthEntry)
    suspend fun deleteEntry(entry: HealthEntry)

    // Reminders
    fun getAllReminders(): Flow<List<Reminder>>
    fun getEnabledReminders(): Flow<List<Reminder>>
    suspend fun getReminderById(id: Long): Reminder?
    suspend fun getRemindersByTypeAndName(type: EntryType?, name: String): List<Reminder>
    suspend fun insertReminder(reminder: Reminder): Long
    suspend fun updateReminder(reminder: Reminder)
    suspend fun deleteReminder(reminder: Reminder)
}
