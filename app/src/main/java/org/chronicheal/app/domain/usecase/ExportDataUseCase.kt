package org.chronicheal.app.domain.usecase

import kotlinx.coroutines.flow.first
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.chronicheal.app.domain.repository.EntryRepository
import javax.inject.Inject

class ExportDataUseCase @Inject constructor(
    private val repository: EntryRepository
) {
    private val json = Json { prettyPrint = true }

    suspend operator fun invoke(): String {
        val entries = repository.getAllEntries().first()
        return json.encodeToString(entries)
    }
}
