package org.chronicheal.app.domain.usecase

import kotlinx.coroutines.flow.Flow
import org.chronicheal.app.domain.model.HealthEntry
import org.chronicheal.app.domain.repository.EntryRepository
import javax.inject.Inject

class GetEntriesUseCase @Inject constructor(
    private val repository: EntryRepository
) {
    operator fun invoke(): Flow<List<HealthEntry>> {
        return repository.getAllEntries()
    }
}
