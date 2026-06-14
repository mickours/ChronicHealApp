package org.chronicheal.app.domain.usecase

import org.chronicheal.app.domain.model.Reminder
import org.chronicheal.app.domain.repository.HealthRepository
import javax.inject.Inject

class GetReminderByIdUseCase @Inject constructor(
    private val repository: HealthRepository
) {
    suspend operator fun invoke(id: Long): Reminder? {
        return repository.getReminderById(id)
    }
}
