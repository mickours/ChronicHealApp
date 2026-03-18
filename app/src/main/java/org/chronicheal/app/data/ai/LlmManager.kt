package org.chronicheal.app.data.ai

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.net.ConnectivityManager
import android.os.Build
import android.util.Log
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import org.chronicheal.app.domain.model.AiMealAnalysis
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LlmManager @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private val tag = "LlmManager"
    private val channelId = "ai_model_download"

    // Model configuration for the local file provided
    private val modelFileName = "gemma3-1b-it-int4.task"
    private val modelFile = File("/data/local/tmp/llm/$modelFileName")

    private var llmInference: LlmInference? = null

    private val _isDownloading = MutableStateFlow(false)
    val isDownloading: StateFlow<Boolean> = _isDownloading

    private val _downloadProgress = MutableStateFlow(0f)
    val downloadProgress: StateFlow<Float> = _downloadProgress

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "AI Model Download"
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(channelId, name, importance)
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * For local development/testing with the provided path.
     * Note: In production, apps usually can't access /data/local/tmp
     */
    fun isModelPresent(): Boolean {
        val present = modelFile.exists() && modelFile.canRead()
        Log.d(tag, "Checking model at ${modelFile.absolutePath}, present: $present")
        return present
    }

    fun isMeteredConnection(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        return cm.isActiveNetworkMetered
    }

    suspend fun downloadModel() = withContext(Dispatchers.IO) {
        // Download disabled as requested, using local file in /data/local/tmp
        Log.i(tag, "Download disabled. Please ensure model is at ${modelFile.absolutePath}")
    }

    private fun initInference() {
        if (llmInference != null) return
        if (!isModelPresent()) {
            Log.e(
                tag,
                "Cannot init inference: Model file not found or not readable at ${modelFile.absolutePath}"
            )
            return
        }

        try {
            Log.d(tag, "Initializing LLM with model: ${modelFile.absolutePath}")
            val options = LlmInference.LlmInferenceOptions.builder()
                .setModelPath(modelFile.absolutePath)
                .setMaxTokens(1024)
                .build()

            llmInference = LlmInference.createFromOptions(context, options)
            Log.i(tag, "LLM Inference initialized successfully")
        } catch (e: Exception) {
            Log.e(tag, "LLM Init failed", e)
        }
    }

    suspend fun analyzeMeal(description: String): AiMealAnalysis? =
        withContext(Dispatchers.Default) {
            try {
                initInference()
                val inference = llmInference ?: return@withContext null

                val systemPrompt = """
                You are a nutrition assistant. Analyze the user's meal description and return a JSON object.
                The JSON MUST have the following structure:
                {
                  "name": "Short name of the meal",
                  "ingredients": [
                    {"name": "ingredient name", "quantity": 100.0, "unit": "g"}
                  ],
                  "allergens": ["gluten", "lactose", "egg", "soy", "peanut", "tree_nut", "fish", "shellfish", "sesame", "mustard", "sulfite", "lupin", "mollusk", "celery"],
                  "fodmaps": ["fructans", "gos", "lactose", "fructose", "sorbitol", "mannitol"],
                  "note": "Any additional notes"
                }
                Only include allergens and fodmaps that are definitely or likely present.
                Valid allergens: gluten, lactose, egg, soy, peanut, tree_nut, fish, shellfish, sesame, mustard, sulfite, lupin, mollusk, celery.
                Valid fodmaps: fructans, gos, lactose, fructose, sorbitol, mannitol.
                Return ONLY the JSON.
            """.trimIndent()

                val fullPrompt =
                    "<start_of_turn>user\n$systemPrompt\n\nMeal: $description<end_of_turn>\n<start_of_turn>model\n"

                val response = inference.generateResponse(fullPrompt)

                val jsonStart = response.indexOf("{")
                val jsonEnd = response.lastIndexOf("}") + 1
                if (jsonStart == -1 || jsonEnd == 0) return@withContext null

                val jsonString = response.substring(jsonStart, jsonEnd)
                return@withContext json.decodeFromString<AiMealAnalysis>(jsonString)
            } catch (e: Exception) {
                Log.e(tag, "Analysis failed", e)
                null
            }
        }
}
