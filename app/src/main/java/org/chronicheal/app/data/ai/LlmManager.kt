package org.chronicheal.app.data.ai

import kotlinx.coroutines.flow.StateFlow
import org.chronicheal.app.domain.model.AiMealAnalysis
import org.chronicheal.app.domain.model.HealthEntry

interface LlmManager {
    val isAiEnabled: Boolean
    val isDownloading: StateFlow<Boolean>
    val downloadProgress: StateFlow<Float>

    fun isModelPresent(): Boolean
    fun isMeteredConnection(): Boolean
    suspend fun downloadModel()
    suspend fun analyzeMeal(description: String): AiMealAnalysis?
    suspend fun processLog(text: String): List<HealthEntry>?
}
