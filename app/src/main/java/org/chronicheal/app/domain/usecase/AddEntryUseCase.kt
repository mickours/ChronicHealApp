package org.chronicheal.app.domain.usecase

import org.chronicheal.app.domain.model.HealthEntry
import org.chronicheal.app.domain.repository.HealthRepository
import javax.inject.Inject

class AddEntryUseCase @Inject constructor(
    private val repository: HealthRepository
) {
    suspend operator fun invoke(entry: HealthEntry): Long {
        return repository.insertEntry(entry)
    }
}
