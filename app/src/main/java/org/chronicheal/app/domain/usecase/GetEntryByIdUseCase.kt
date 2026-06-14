package org.chronicheal.app.domain.usecase

import org.chronicheal.app.domain.model.HealthEntry
import org.chronicheal.app.domain.repository.HealthRepository
import javax.inject.Inject

class GetEntryByIdUseCase @Inject constructor(
    private val repository: HealthRepository
) {
    suspend operator fun invoke(id: Long): HealthEntry? {
        return repository.getEntryById(id)
    }
}
