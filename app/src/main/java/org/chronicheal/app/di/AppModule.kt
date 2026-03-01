package org.chronicheal.app.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import org.chronicheal.app.data.local.AppDatabase
import org.chronicheal.app.data.local.EntryDao
import org.chronicheal.app.data.local.ReminderDao
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "chronicheal_db"
        )
        .addMigrations(AppDatabase.MIGRATION_4_5)
        .fallbackToDestructiveMigration()
        .build()
    }

    @Provides
    fun provideEntryDao(database: AppDatabase): EntryDao {
        return database.entryDao()
    }

    @Provides
    fun provideReminderDao(database: AppDatabase): ReminderDao {
        return database.reminderDao()
    }
}
