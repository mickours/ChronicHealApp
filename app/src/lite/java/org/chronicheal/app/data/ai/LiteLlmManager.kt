package org.chronicheal.app.data.ai

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.chronicheal.app.domain.model.AiMealAnalysis
import org.chronicheal.app.domain.model.EntryType
import org.chronicheal.app.domain.model.HealthEntry
import org.chronicheal.app.domain.model.Ingredient
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Locale
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

    override suspend fun processLog(text: String): List<HealthEntry>? {
        if (text.isBlank()) return null

        val lowercaseText = text.lowercase(Locale.ROOT)
        val entries = mutableListOf<HealthEntry>()

        // Extract timestamp relative to now
        var entryTimestamp = Instant.now()

        // Handle "yesterday" / "hier"
        if (lowercaseText.containsAny("yesterday", "hier")) {
            entryTimestamp = entryTimestamp.minus(1, ChronoUnit.DAYS)
        }

        // Handle "X days ago" / "il y a X jours"
        DAYS_AGO_REGEX.find(lowercaseText)?.let { match ->
            val days =
                (match.groupValues[1].ifBlank { null } ?: match.groupValues[2]).toLongOrNull() ?: 0L
            if (days > 0) {
                entryTimestamp = Instant.now().minus(days, ChronoUnit.DAYS)
            }
        }

        // Handle "X hours ago" / "il y a X heures"
        HOURS_AGO_REGEX.find(lowercaseText)?.let { match ->
            val hours =
                (match.groupValues[1].ifBlank { null } ?: match.groupValues[2]).toLongOrNull() ?: 0L
            if (hours > 0) {
                entryTimestamp = entryTimestamp.minus(hours, ChronoUnit.HOURS)
            }
        }

        // Handle "X minutes ago" / "il y a X minutes"
        MINS_AGO_REGEX.find(lowercaseText)?.let { match ->
            val mins =
                (match.groupValues[1].ifBlank { null } ?: match.groupValues[2]).toLongOrNull() ?: 0L
            if (mins > 0) {
                entryTimestamp = entryTimestamp.minus(mins, ChronoUnit.MINUTES)
            }
        }

        // Simple keyword-based categorization for the Lite version
        val detectedTypes = mutableSetOf<EntryType>()

        if (lowercaseText.containsAny("pain", "hurt", "ache", "mal", "douleur")) {
            detectedTypes.add(EntryType.PAIN)
        }
        if (lowercaseText.containsAny("took", "drug", "pill", "medication", "pris", "médicament")) {
            detectedTypes.add(EntryType.DRUG)
        }
        if (lowercaseText.containsAny("meal", "ate", "food", "repas", "mangé")) {
            detectedTypes.add(EntryType.MEAL)
        }
        if (lowercaseText.containsAny("sleep", "slept", "dormi", "sommeil")) {
            detectedTypes.add(EntryType.SLEEP)
        }
        if (lowercaseText.containsAny("mood", "feel", "sentiment", "humeur")) {
            detectedTypes.add(EntryType.MOOD)
        }
        if (lowercaseText.containsAny("symptom", "nausea", "fatigue", "symptôme")) {
            detectedTypes.add(EntryType.SYMPTOM)
        }
        if (lowercaseText.containsAny(
                "beverage",
                "drank",
                "water",
                "coffee",
                "tea",
                "boisson",
                "bu",
                "eau",
                "café",
                "thé"
            )
        ) {
            detectedTypes.add(EntryType.BEVERAGE)
        }
        if (lowercaseText.containsAny("stool", "bowel", "poop", "selle", "caca")) {
            detectedTypes.add(EntryType.STOOL)
        }
        if (lowercaseText.containsAny("period", "bleeding", "règles", "flux")) {
            detectedTypes.add(EntryType.PERIOD)
        }
        if (lowercaseText.containsAny(
                "activity",
                "exercise",
                "walk",
                "run",
                "sport",
                "activité",
                "exercice",
                "marche",
                "course"
            )
        ) {
            detectedTypes.add(EntryType.ACTIVITY)
        }
        if (lowercaseText.containsAny(
                "appointment",
                "doctor",
                "dentist",
                "rdv",
                "rendez-vous",
                "docteur",
                "médecin"
            )
        ) {
            detectedTypes.add(EntryType.MEDICAL_APPOINTMENT)
        }

        if (detectedTypes.isEmpty()) {
            // Default to Journal if no keywords detected
            entries.add(
                HealthEntry(
                    type = EntryType.JOURNAL,
                    note = text,
                    timestamp = entryTimestamp
                )
            )
        } else {
            detectedTypes.forEach { type ->
                var isAlcoholic: Boolean? = null
                var isCaffeinated: Boolean? = null
                var name: String? = null
                var value: Double? = null
                var unit: String? = null
                var ingredients: List<Ingredient>? = null

                if (type == EntryType.BEVERAGE) {
                    if (lowercaseText.containsAny(
                            "bière",
                            "vin",
                            "beer",
                            "wine",
                            "alcool",
                            "alcohol"
                        )
                    ) {
                        isAlcoholic = true
                    }
                    if (lowercaseText.containsAny(
                            "café",
                            "thé",
                            "coffee",
                            "tea",
                            "caffeine",
                            "caféine"
                        )
                    ) {
                        isCaffeinated = true
                    }

                    // Extract name (first match wins)
                    name = BEVERAGE_NAMES.find { lowercaseText.contains(it) }
                        ?.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }

                    // Extract volume (simple regex for "250ml", "1 glass", etc.)
                    VOLUME_REGEX.find(lowercaseText)?.let { match ->
                        value = match.groupValues[1].toDoubleOrNull()
                        unit = match.groupValues[2]
                    }
                }

                if (type == EntryType.MEAL) {
                    // Extract meal name
                    name = MEAL_NAMES.find { lowercaseText.contains(it) }
                        ?.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }

                    // Extract ingredients
                    // Looking for structures like "avec [quelque chose]", "contient [quelque chose]", "mangé [quelque chose]"
                    // This is a simple heuristic for the Lite version
                    val parts = lowercaseText.split(
                        "avec",
                        "contient",
                        "composed of",
                        "consisted of",
                        ":",
                        ","
                    )
                    if (parts.size > 1) {
                        ingredients = parts.drop(1)
                            .map { it.trim() }
                            .filter { it.isNotBlank() && it.length < 50 }
                            .map { ingredientText ->
                                // Try to extract quantity if present (e.g., "100g de poulet" or "2 eggs")
                                val qtyMatch = QTY_REGEX.find(ingredientText)
                                if (qtyMatch != null) {
                                    Ingredient(
                                        name = qtyMatch.groupValues[3].trim()
                                            .ifBlank { qtyMatch.groupValues[0] }
                                            .replaceFirstChar {
                                                if (it.isLowerCase()) it.titlecase(
                                                    Locale.ROOT
                                                ) else it.toString()
                                            },
                                        quantity = qtyMatch.groupValues[1].replace(",", ".")
                                            .toDoubleOrNull(),
                                        unit = qtyMatch.groupValues[2].ifBlank { null }
                                    )
                                } else {
                                    Ingredient(name = ingredientText.replaceFirstChar {
                                        if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString()
                                    })
                                }
                            }
                    }
                }

                entries.add(
                    HealthEntry(
                        type = type,
                        note = text,
                        timestamp = entryTimestamp,
                        isAlcoholic = isAlcoholic,
                        isCaffeinated = isCaffeinated,
                        name = name,
                        value = value,
                        unit = unit,
                        ingredients = ingredients
                    )
                )
            }
        }

        return entries
    }

    private fun String.containsAny(vararg keywords: String): Boolean {
        return keywords.any(this::contains)
    }

    companion object {
        private val DAYS_AGO_REGEX =
            Regex("""(\d+)\s*(?:days?|jours?)\s*ago|il\s*y\s*a\s*(\d+)\s*(?:days?|jours?)""")
        private val HOURS_AGO_REGEX =
            Regex("""(\d+)\s*(?:hours?|heures?|h)\s*ago|il\s*y\s*a\s*(\d+)\s*(?:hours?|heures?|h)""")
        private val MINS_AGO_REGEX =
            Regex("""(\d+)\s*(?:minutes?|mins?|m)\s*ago|il\s*y\s*a\s*(\d+)\s*(?:minutes?|mins?|m)""")
        private val VOLUME_REGEX =
            Regex("""(\d+)\s*(ml|cl|l|verre|glass|tasse|cup|bouteille|bottle)""")
        private val QTY_REGEX =
            Regex("""(\d+(?:[.,]\d+)?)\s*(g|kg|oz|lbs|unité|unités|oeuf|oeufs|egg|eggs)?(?:\s+de\s+|\s+)?(.*)""")

        private val BEVERAGE_NAMES = listOf(
            "café", "coffee", "thé", "tea", "bière", "beer", "vin", "wine",
            "eau", "water", "jus", "juice", "soda", "lait", "milk"
        )
        private val MEAL_NAMES = listOf(
            "petit-déjeuner", "breakfast", "déjeuner", "lunch", "dîner", "dinner",
            "goûter", "snack", "collation"
        )
    }
}
