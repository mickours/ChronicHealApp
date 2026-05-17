package org.chronicheal.app.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import org.chronicheal.app.domain.model.EntryType
import org.chronicheal.app.domain.model.Reminder

@Dao
interface ReminderDao {
    @Query("SELECT * FROM reminders ORDER BY time ASC")
    fun getAllReminders(): Flow<List<Reminder>>

    @Query("SELECT * FROM reminders WHERE isEnabled = 1 ORDER BY time ASC")
    fun getEnabledReminders(): Flow<List<Reminder>>

    @Query("SELECT * FROM reminders WHERE id = :id")
    suspend fun getReminderById(id: Long): Reminder?

    @Query("SELECT * FROM reminders WHERE entryType = :type AND title LIKE '%' || :name || '%'")
    suspend fun getRemindersByTypeAndName(type: EntryType, name: String): List<Reminder>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: Reminder): Long

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateReminder(reminder: Reminder)

    @Delete
    suspend fun deleteReminder(reminder: Reminder)
}
