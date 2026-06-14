package org.chronicheal.app.domain.usecase

import org.chronicheal.app.data.ai.LlmManager
import org.chronicheal.app.domain.model.AiMealAnalysis
import javax.inject.Inject

class AnalyzeMealUseCase @Inject constructor(
    private val llmManager: LlmManager
) {
    suspend operator fun invoke(description: String): AiMealAnalysis? {
        return llmManager.analyzeMeal(description)
    }
}
