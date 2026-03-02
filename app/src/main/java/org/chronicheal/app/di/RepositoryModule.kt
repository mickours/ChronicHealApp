package org.chronicheal.app.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import org.chronicheal.app.data.repository.EntryRepositoryImpl
import org.chronicheal.app.data.repository.ReminderRepositoryImpl
import org.chronicheal.app.data.repository.SecurityRepositoryImpl
import org.chronicheal.app.data.repository.SettingsRepositoryImpl
import org.chronicheal.app.domain.repository.EntryRepository
import org.chronicheal.app.domain.repository.ReminderRepository
import org.chronicheal.app.domain.repository.SecurityRepository
import org.chronicheal.app.domain.repository.SettingsRepository
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

    @Binds
    @Singleton
    abstract fun bindSecurityRepository(
        securityRepositoryImpl: SecurityRepositoryImpl
    ): SecurityRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(
        settingsRepositoryImpl: SettingsRepositoryImpl
    ): SettingsRepository
}
