package org.chronicheal.app.presentation.util

import android.content.Context
import org.chronicheal.app.R
import org.chronicheal.app.domain.model.EntryType
import org.chronicheal.app.domain.model.HealthEntry
import java.time.Instant
import java.util.Locale

class VoiceCommandParser(private val context: Context) {

    fun parse(text: String): List<HealthEntry> {
        val lowerText = text.lowercase(Locale.getDefault())
        
        // Split by common conjunctions to handle multiple entries
        val parts = lowerText.split(Regex("\\b(and|with|et|avec|,)\\b"))
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        if (parts.size <= 1) {
            return listOf(parseSingle(lowerText, text))
        }

        return parts.map { part -> 
            parseSingle(part, part)
        }
    }

    private fun parseSingle(lowerText: String, originalText: String): HealthEntry {
        // 1. Determine Entry Type using localized keywords
        val type = when {
            containsAny(lowerText, R.string.voice_keywords_pain) -> EntryType.PAIN
            containsAny(lowerText, R.string.voice_keywords_drug) -> EntryType.DRUG
            containsAny(lowerText, R.string.voice_keywords_symptom) -> EntryType.SYMPTOM
            containsAny(lowerText, R.string.voice_keywords_sleep) -> EntryType.SLEEP
            containsAny(lowerText, R.string.voice_keywords_mood) -> EntryType.MOOD
            containsAny(lowerText, R.string.voice_keywords_beverage) -> EntryType.BEVERAGE
            containsAny(lowerText, R.string.voice_keywords_meal) -> EntryType.MEAL
            containsAny(lowerText, R.string.voice_keywords_activity) -> EntryType.ACTIVITY
            containsAny(lowerText, R.string.voice_keywords_disease) -> EntryType.DISEASE
            containsAny(lowerText, R.string.voice_keywords_appointment) -> EntryType.MEDICAL_APPOINTMENT
            containsAny(lowerText, R.string.voice_keywords_factor) -> EntryType.EXTERNAL_FACTOR
            containsAny(lowerText, R.string.voice_keywords_period) -> EntryType.PERIOD
            containsAny(lowerText, R.string.voice_keywords_stool) -> EntryType.STOOL
            else -> EntryType.JOURNAL // Fallback
        }

        // 2. Extract Intensity (1-10)
        val intensity = extractNumber(lowerText, context.getString(R.string.voice_keywords_intensity).replace(",", "|")) 
            ?: extractSimpleNumber(lowerText)

        // 3. Extract Duration (for sleep/activity)
        val duration = if (type == EntryType.SLEEP || type == EntryType.ACTIVITY) {
            extractDurationMinutes(lowerText)
        } else null

        // 4. Extract Name/Location/Note
        var name: String? = null
        var location: String? = null
        var unit: String? = null
        var value: Double? = null

        when (type) {
            EntryType.PAIN, EntryType.SYMPTOM -> {
                location = extractLocation(lowerText)
                if (type == EntryType.SYMPTOM) {
                    name = extractFromList(lowerText, R.string.voice_symptoms_list)
                }
            }
            EntryType.DRUG -> {
                name = extractEntityName(lowerText, R.string.voice_markers_drug)
                unit = extractDosage(lowerText)
            }
            EntryType.BEVERAGE -> {
                name = extractFromList(lowerText, R.string.voice_beverages_list)
                value = extractQuantity(lowerText)
            }
            EntryType.DISEASE -> {
                name = extractFromList(lowerText, R.string.voice_diseases_list)
            }
            EntryType.MEDICAL_APPOINTMENT -> {
                name = extractEntityName(lowerText, R.string.voice_markers_doctor)
            }
            EntryType.EXTERNAL_FACTOR -> {
                name = extractFromList(lowerText, R.string.voice_factors_list)
            }
            EntryType.ACTIVITY -> {
                name = extractFromList(lowerText, R.string.voice_activities_list)
            }
            EntryType.STOOL -> {
                name = extractFromList(lowerText, R.string.voice_aspects_list)
            }
            EntryType.MEAL -> {
                name = extractFromList(lowerText, R.string.voice_meals_list)
            }
            else -> {}
        }

        return HealthEntry(
            timestamp = Instant.now(),
            type = type,
            intensity = intensity,
            name = name,
            location = location,
            durationMinutes = duration,
            unit = unit,
            value = value,
            note = originalText
        )
    }

    private fun containsAny(text: String, resId: Int): Boolean {
        val keywords = context.getString(resId).split(",")
        return keywords.any { text.contains(it.trim()) }
    }

    private fun extractNumber(text: String, keywords: String): Int? {
        val regex = Regex("($keywords)\\s*(\\d+)")
        return regex.find(text)?.groupValues?.get(2)?.toIntOrNull()
    }

    private fun extractSimpleNumber(text: String): Int? {
        val regex = Regex("\\b(\\d+)\\b")
        return regex.find(text)?.groupValues?.get(1)?.toIntOrNull()?.coerceIn(1, 10)
    }

    private fun extractDurationMinutes(text: String): Int? {
        val hoursKeywords = context.getString(R.string.voice_keywords_duration_hours).replace(",", "|")
        val minsKeywords = context.getString(R.string.voice_keywords_duration_mins).replace(",", "|")
        
        val hoursRegex = Regex("(\\d+)\\s*($hoursKeywords)")
        val minsRegex = Regex("(\\d+)\\s*($minsKeywords)")
        
        val hours = hoursRegex.find(text)?.groupValues?.get(1)?.toIntOrNull() ?: 0
        val mins = minsRegex.find(text)?.groupValues?.get(1)?.toIntOrNull() ?: 0
        
        return if (hours > 0 || mins > 0) hours * 60 + mins else null
    }

    private fun extractLocation(text: String): String? {
        val mappingStr = context.getString(R.string.voice_locations_map)
        val locationsMap = mappingStr.split(",").associate {
            val parts = it.split(":")
            if (parts.size == 2) {
                parts[0].trim() to parts[1].trim()
            } else {
                it.trim() to it.trim()
            }
        }
        return locationsMap.keys.find { text.contains(it) }?.let { locationsMap[it] }
    }

    private fun extractEntityName(text: String, markersResId: Int): String? {
        val markers = context.getString(markersResId).split(",")
        for (marker in markers) {
            val markerTrimmed = marker.trim()
            val index = text.indexOf(markerTrimmed)
            if (index != -1) {
                val remaining = text.substring(index + markerTrimmed.length).trim()
                val firstWord = remaining.split(" ").firstOrNull()
                if (firstWord != null && firstWord.isNotEmpty() && !firstWord[0].isDigit()) {
                    return firstWord.replaceFirstChar { it.uppercase() }
                }
            }
        }
        return null
    }

    private fun extractDosage(text: String): String? {
        val units = context.getString(R.string.voice_dosage_units).replace(",", "|")
        val regex = Regex("(\\d+)\\s*($units)")
        return regex.find(text)?.value
    }

    private fun extractFromList(text: String, listResId: Int): String? {
        val items = context.getString(listResId).split(",")
        return items.find { text.contains(it.trim()) }?.trim()?.replaceFirstChar { it.uppercase() }
    }

    private fun extractQuantity(text: String): Double? {
        val containers = context.getString(R.string.voice_beverage_containers).replace(",", "|")
        val regex = Regex("(\\d+)\\s*($containers)")
        return regex.find(text)?.groupValues?.get(1)?.toDoubleOrNull()
    }
}
