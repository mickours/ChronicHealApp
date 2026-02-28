package org.chronicheal.app.domain.repository

import kotlinx.coroutines.flow.Flow
import org.chronicheal.app.domain.model.HealthEntry

interface EntryRepository {
    fun getAllEntries(): Flow<List<HealthEntry>>
    suspend fun getEntryById(id: Long): HealthEntry?
    suspend fun insertEntry(entry: HealthEntry)
    suspend fun updateEntry(entry: HealthEntry)
    suspend fun deleteEntry(entry: HealthEntry)
}
