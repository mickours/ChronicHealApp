package org.chronicheal.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import org.chronicheal.app.domain.model.HealthEntry
import org.chronicheal.app.domain.model.Reminder

@Database(entities = [HealthEntry::class, Reminder::class], version = 11, exportSchema = true)
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

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE health_entries ADD COLUMN origin TEXT")
            }
        }

        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE health_entries ADD COLUMN fodmaps TEXT")
            }
        }

        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE health_entries ADD COLUMN proteins REAL")
                db.execSQL("ALTER TABLE health_entries ADD COLUMN carbohydrates REAL")
                db.execSQL("ALTER TABLE health_entries ADD COLUMN lipids REAL")
            }
        }

        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE reminders ADD COLUMN templateEntryId INTEGER")
            }
        }
    }
}
