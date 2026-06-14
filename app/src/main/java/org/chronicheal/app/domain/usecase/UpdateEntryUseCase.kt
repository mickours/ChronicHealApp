package org.chronicheal.app.domain.usecase

import org.chronicheal.app.domain.model.HealthEntry
import org.chronicheal.app.domain.repository.HealthRepository
import javax.inject.Inject

class UpdateEntryUseCase @Inject constructor(
    private val repository: HealthRepository
) {
    suspend operator fun invoke(entry: HealthEntry) {
        repository.updateEntry(entry)
    }
}
