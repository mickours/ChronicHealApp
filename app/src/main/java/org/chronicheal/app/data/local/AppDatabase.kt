package org.chronicheal.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import org.chronicheal.app.domain.model.HealthEntry
import org.chronicheal.app.domain.model.Reminder

@Database(entities = [HealthEntry::class, Reminder::class], version = 4, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun entryDao(): EntryDao
    abstract fun reminderDao(): ReminderDao
}
