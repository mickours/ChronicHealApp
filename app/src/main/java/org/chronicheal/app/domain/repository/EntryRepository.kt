package org.chronicheal.app.domain.repository

import kotlinx.coroutines.flow.Flow
import org.chronicheal.app.domain.model.EntryType
import org.chronicheal.app.domain.model.HealthEntry
import java.time.Instant

interface EntryRepository {
    fun getAllEntries(): Flow<List<HealthEntry>>
    suspend fun getEntryById(id: Long): HealthEntry?
    suspend fun getEntryByReminderId(reminderId: Long): HealthEntry?
    suspend fun getLastEntryByTypeAndName(type: EntryType, name: String): HealthEntry?
    suspend fun getEntriesSince(since: Instant): List<HealthEntry>
    suspend fun insertEntry(entry: HealthEntry)
    suspend fun updateEntry(entry: HealthEntry)
    suspend fun deleteEntry(entry: HealthEntry)
    suspend fun insertEntries(entries: List<HealthEntry>)
}
