package org.chronicheal.app.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import org.chronicheal.app.data.repository.EntryRepositoryImpl
import org.chronicheal.app.data.repository.ReminderRepositoryImpl
import org.chronicheal.app.domain.repository.EntryRepository
import org.chronicheal.app.domain.repository.ReminderRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindEntryRepository(
        entryRepositoryImpl: EntryRepositoryImpl
    ): EntryRepository

    @Binds
    @Singleton
    abstract fun bindReminderRepository(
        reminderRepositoryImpl: ReminderRepositoryImpl
    ): ReminderRepository
}
