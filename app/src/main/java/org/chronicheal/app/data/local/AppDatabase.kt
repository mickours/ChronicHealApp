package org.chronicheal.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import org.chronicheal.app.domain.model.HealthEntry
import org.chronicheal.app.domain.model.Reminder

@Database(entities = [HealthEntry::class, Reminder::class], version = 7, exportSchema = true)
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

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE health_entries ADD COLUMN ingredients TEXT")
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE health_entries ADD COLUMN isAlcoholic INTEGER")
                db.execSQL("ALTER TABLE health_entries ADD COLUMN isCaffeinated INTEGER")
                db.execSQL("ALTER TABLE health_entries ADD COLUMN allergens TEXT")
            }
        }
    }
}
