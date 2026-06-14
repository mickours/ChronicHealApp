package org.chronicheal.app.data.ai

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.chronicheal.app.domain.model.AiMealAnalysis
import org.chronicheal.app.domain.model.HealthEntry
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LiteLlmManager @Inject constructor() : LlmManager {
    override val isAiEnabled: Boolean = false
    override val isDownloading: StateFlow<Boolean> = MutableStateFlow(false)
    override val downloadProgress: StateFlow<Float> = MutableStateFlow(0f)

    override fun isModelPresent(): Boolean = false
    override fun isMeteredConnection(): Boolean = false
    override suspend fun downloadModel() {}
    override suspend fun analyzeMeal(description: String): AiMealAnalysis? = null
    override suspend fun processLog(text: String): List<HealthEntry>? = null
}
