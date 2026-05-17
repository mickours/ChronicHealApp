package org.chronicheal.app.data.repository

import kotlinx.coroutines.flow.Flow
import org.chronicheal.app.data.local.EntryDao
import org.chronicheal.app.domain.model.EntryType
import org.chronicheal.app.domain.model.HealthEntry
import org.chronicheal.app.domain.repository.EntryRepository
import java.time.Instant
import javax.inject.Inject

class EntryRepositoryImpl @Inject constructor(
    private val dao: EntryDao
) : EntryRepository {

    override fun getAllEntries(): Flow<List<HealthEntry>> {
        return dao.getAllEntries()
    }

    override suspend fun getEntryById(id: Long): HealthEntry? {
        return dao.getEntryById(id)
    }

    override suspend fun getEntryByReminderId(reminderId: Long): HealthEntry? {
        return dao.getEntryByReminderId(reminderId)
    }

    override suspend fun getLastEntryByTypeAndName(type: EntryType, name: String): HealthEntry? {
        return dao.getLastEntryByTypeAndName(type, name)
    }

    override suspend fun getEntriesSince(since: Instant): List<HealthEntry> {
        return dao.getEntriesSince(since)
    }

    override suspend fun insertEntry(entry: HealthEntry): Long {
        return dao.insertEntry(entry)
    }

    override suspend fun insertEntries(entries: List<HealthEntry>) {
        dao.insertEntries(entries)
    }

    override suspend fun updateEntry(entry: HealthEntry) {
        dao.updateEntry(entry)
    }

    override suspend fun deleteEntry(entry: HealthEntry) {
        dao.deleteEntry(entry)
    }
}
