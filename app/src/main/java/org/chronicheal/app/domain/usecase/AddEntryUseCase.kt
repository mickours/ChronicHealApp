package org.chronicheal.app.domain.usecase

import org.chronicheal.app.domain.model.HealthEntry
import org.chronicheal.app.domain.repository.EntryRepository
import javax.inject.Inject

class AddEntryUseCase @Inject constructor(
    private val repository: EntryRepository
) {
    suspend operator fun invoke(entry: HealthEntry) {
        repository.insertEntry(entry)
    }
}
