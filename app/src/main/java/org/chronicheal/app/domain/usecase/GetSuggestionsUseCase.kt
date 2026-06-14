package org.chronicheal.app.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.chronicheal.app.domain.model.EntryType
import org.chronicheal.app.domain.model.HealthEntry
import org.chronicheal.app.domain.repository.HealthRepository
import javax.inject.Inject

class GetSuggestionsUseCase @Inject constructor(
    private val repository: HealthRepository
) {
    fun execute(
        types: Set<EntryType>,
        field: SuggestionField,
        parentLocation: String? = null
    ): Flow<List<String>> {
        return repository.getAllEntries().map { entries ->
            entries
                .filter { entry ->
                    (types.isEmpty() || entry.type in types) &&
                            (parentLocation == null || entry.location == parentLocation)
                }
                .mapNotNull { entry ->
                    when (field) {
                        SuggestionField.NAME -> entry.name
                        SuggestionField.LOCATION -> entry.location
                        SuggestionField.ORIGIN -> entry.origin
                        SuggestionField.UNIT -> entry.unit
                    }
                }
                .filter { it.isNotBlank() }
                .groupBy { it }
                .mapValues { it.value.size }
                .toList()
                .sortedByDescending { it.second }
                .map { it.first }
        }
    }

    enum class SuggestionField {
        NAME, LOCATION, ORIGIN, UNIT
    }
}
