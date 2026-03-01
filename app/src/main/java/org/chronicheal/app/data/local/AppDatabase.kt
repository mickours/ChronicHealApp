package org.chronicheal.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import org.chronicheal.app.domain.model.HealthEntry
import org.chronicheal.app.domain.model.Reminder

@Database(entities = [HealthEntry::class, Reminder::class], version = 5, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun entryDao(): EntryDao
    abstract fun reminderDao(): ReminderDao

    companion object {
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE health_entries ADD COLUMN durationMinutes INTEGER")
                db.execSQL("ALTER TABLE health_entries ADD COLUMN isFinished INTEGER NOT NULL DEFAULT 0")
            }
        }
    }
}
