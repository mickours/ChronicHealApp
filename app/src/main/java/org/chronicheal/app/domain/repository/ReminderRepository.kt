package org.chronicheal.app.domain.repository

import kotlinx.coroutines.flow.Flow
import org.chronicheal.app.domain.model.Reminder

interface ReminderRepository {
    fun getAllReminders(): Flow<List<Reminder>>
    fun getEnabledReminders(): Flow<List<Reminder>>
    suspend fun getReminderById(id: Long): Reminder?
    suspend fun insertReminder(reminder: Reminder): Long
    suspend fun updateReminder(reminder: Reminder)
    suspend fun deleteReminder(reminder: Reminder)
}
