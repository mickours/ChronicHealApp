package org.chronicheal.app.data.repository

import kotlinx.coroutines.flow.Flow
import org.chronicheal.app.data.local.EntryDao
import org.chronicheal.app.domain.model.HealthEntry
import org.chronicheal.app.domain.repository.EntryRepository
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

    override suspend fun insertEntry(entry: HealthEntry) {
        dao.insertEntry(entry)
    }

    override suspend fun updateEntry(entry: HealthEntry) {
        dao.updateEntry(entry)
    }

    override suspend fun deleteEntry(entry: HealthEntry) {
        dao.deleteEntry(entry)
    }
}
