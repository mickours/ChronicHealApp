package org.chronicheal.app.presentation.util

import org.chronicheal.app.domain.model.EntryType
import org.chronicheal.app.domain.model.HealthEntry
import java.time.Instant
import java.util.Locale

class VoiceCommandParser {

    fun parse(text: String): HealthEntry? {
        val lowerText = text.lowercase(Locale.getDefault())
        
        // 1. Determine Entry Type
        val type = when {
            lowerText.contains("pain") || lowerText.contains("hurt") || lowerText.contains("ache") -> EntryType.PAIN
            lowerText.contains("took") || lowerText.contains("medication") || lowerText.contains("drug") || lowerText.contains("pill") -> EntryType.DRUG
            lowerText.contains("symptom") || (lowerText.contains("feeling") && (lowerText.contains("nausea") || lowerText.contains("fatigue") || lowerText.contains("dizzy"))) -> EntryType.SYMPTOM
            lowerText.contains("slept") || lowerText.contains("sleep") -> EntryType.SLEEP
            lowerText.contains("mood") || lowerText.contains("feel") || lowerText.contains("feeling") -> EntryType.MOOD
            lowerText.contains("drank") || lowerText.contains("beverage") || lowerText.contains("water") || lowerText.contains("coffee") -> EntryType.BEVERAGE
            lowerText.contains("ate") || lowerText.contains("meal") || lowerText.contains("food") -> EntryType.MEAL
            lowerText.contains("activity") || lowerText.contains("exercise") || lowerText.contains("ran") || lowerText.contains("walked") -> EntryType.ACTIVITY
            lowerText.contains("disease") || lowerText.contains("condition") || lowerText.contains("diagnosed") -> EntryType.DISEASE
            lowerText.contains("appointment") || lowerText.contains("doctor") || lowerText.contains("visit") -> EntryType.MEDICAL_APPOINTMENT
            lowerText.contains("weather") || lowerText.contains("factor") || lowerText.contains("stress") || lowerText.contains("pollution") -> EntryType.EXTERNAL_FACTOR
            lowerText.contains("period") || lowerText.contains("menstruation") || lowerText.contains("flow") -> EntryType.PERIOD
            lowerText.contains("stool") || lowerText.contains("bowel") || lowerText.contains("poop") -> EntryType.STOOL
            else -> EntryType.JOURNAL // Fallback
        }

        // 2. Extract Intensity (1-10)
        val intensity = extractNumber(lowerText, "level|intensity|severity|quality|score|flow|impact") ?: extractSimpleNumber(lowerText)

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
                    name = extractSymptomName(lowerText)
                }
            }
            EntryType.DRUG -> {
                name = extractDrugName(lowerText)
                unit = extractDosage(lowerText)
            }
            EntryType.BEVERAGE -> {
                name = extractBeverageName(lowerText)
                value = extractQuantity(lowerText)
            }
            EntryType.DISEASE -> {
                name = extractDiseaseName(lowerText)
            }
            EntryType.MEDICAL_APPOINTMENT -> {
                name = extractDoctorName(lowerText)
            }
            EntryType.EXTERNAL_FACTOR -> {
                name = extractFactorName(lowerText)
            }
            EntryType.ACTIVITY -> {
                name = extractActivityName(lowerText)
            }
            EntryType.STOOL -> {
                name = extractStoolAspect(lowerText)
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
            note = text
        )
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
        val hoursRegex = Regex("(\\d+)\\s*(hour|hr|h)")
        val minsRegex = Regex("(\\d+)\\s*(minute|min|m)")
        
        val hours = hoursRegex.find(text)?.groupValues?.get(1)?.toIntOrNull() ?: 0
        val mins = minsRegex.find(text)?.groupValues?.get(1)?.toIntOrNull() ?: 0
        
        return if (hours > 0 || mins > 0) hours * 60 + mins else null
    }

    private fun extractLocation(text: String): String? {
        val locations = listOf("back", "head", "knee", "shoulder", "arm", "leg", "neck", "stomach", "chest", "wrist", "ankle", "hip")
        return locations.find { text.contains(it) }?.replaceFirstChar { it.uppercase() }
    }

    private fun extractDrugName(text: String): String? {
        val markers = listOf("took", "medication", "drug", "pill")
        for (marker in markers) {
            val index = text.indexOf(marker)
            if (index != -1) {
                val remaining = text.substring(index + marker.length).trim()
                val firstWord = remaining.split(" ").firstOrNull()
                if (firstWord != null && !firstWord[0].isDigit()) {
                    return firstWord.replaceFirstChar { it.uppercase() }
                }
            }
        }
        return null
    }

    private fun extractDosage(text: String): String? {
        val regex = Regex("(\\d+)\\s*(mg|ml|g|tablets|pills|units|mcg)")
        return regex.find(text)?.value
    }

    private fun extractBeverageName(text: String): String? {
        val names = listOf("water", "coffee", "tea", "juice", "soda", "beer", "wine", "milk", "shake")
        return names.find { text.contains(it) }?.replaceFirstChar { it.uppercase() }
    }

    private fun extractQuantity(text: String): Double? {
        val regex = Regex("(\\d+)\\s*(ml|oz|glass|cup|bottle)")
        return regex.find(text)?.groupValues?.get(1)?.toDoubleOrNull()
    }

    private fun extractSymptomName(text: String): String? {
        val symptoms = listOf("nausea", "fatigue", "dizzy", "headache", "itchy", "rash", "fever", "cough", "congestion")
        return symptoms.find { text.contains(it) }?.replaceFirstChar { it.uppercase() }
    }

    private fun extractDiseaseName(text: String): String? {
        val diseases = listOf("flu", "cold", "covid", "diabetes", "asthma", "migraine", "arthritis")
        return diseases.find { text.contains(it) }?.replaceFirstChar { it.uppercase() }
    }

    private fun extractDoctorName(text: String): String? {
        val regex = Regex("(doctor|dr|dr\\.)\\s*([a-z]+)")
        return regex.find(text)?.groupValues?.get(2)?.replaceFirstChar { it.uppercase() }
    }

    private fun extractFactorName(text: String): String? {
        val factors = listOf("stress", "weather", "heat", "cold", "pollution", "humidity")
        return factors.find { text.contains(it) }?.replaceFirstChar { it.uppercase() }
    }

    private fun extractActivityName(text: String): String? {
        val activities = listOf("walking", "running", "swimming", "cycling", "yoga", "gym", "workout")
        return activities.find { text.contains(it) }?.replaceFirstChar { it.uppercase() }
    }

    private fun extractStoolAspect(text: String): String? {
        val aspects = listOf("hard", "soft", "liquid", "normal", "bloody", "mucus")
        return aspects.find { text.contains(it) }?.replaceFirstChar { it.uppercase() }
    }
}
