package org.chronicheal.app.domain.usecase

import kotlinx.serialization.json.Json
import org.chronicheal.app.domain.model.HealthEntry
import org.chronicheal.app.domain.repository.EntryRepository
import javax.inject.Inject

class ImportDataUseCase @Inject constructor(
    private val repository: EntryRepository
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend operator fun invoke(jsonData: String) {
        val entries = json.decodeFromString<List<HealthEntry>>(jsonData)
        repository.insertEntries(entries)
    }
}
