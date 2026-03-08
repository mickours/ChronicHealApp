package org.chronicheal.app.domain.model

import kotlinx.serialization.Serializable
import org.chronicheal.app.R

@Serializable
enum class EntryType(val emoji: String, val displayRes: Int) {
    PAIN("💥", R.string.type_pain),
    DRUG("💊", R.string.type_drug),
    SYMPTOM("🌡️", R.string.type_symptom),
    DISEASE("🏥", R.string.type_disease),
    MEAL("🍲", R.string.type_meal),
    SLEEP("😴", R.string.type_sleep),
    MEDICAL_APPOINTMENT("👨‍⚕️", R.string.type_appointment),
    ACTIVITY("🏃", R.string.type_activity),
    EXTERNAL_FACTOR("☁️", R.string.type_factor),
    JOURNAL("📝", R.string.type_journal),
    PERIOD("🩸", R.string.type_period),
    BEVERAGE("☕", R.string.type_beverage),
    STOOL("💩", R.string.type_stool),
    MOOD("😊", R.string.type_mood),
    VOICE_LOGGING("🎙️", R.string.voice_logging_title);

    enum class Category(val titleRes: Int) {
        OCCURRENCE(R.string.category_occurrence), // "What occurs to you"
        MANAGEMENT(R.string.category_management)  // "What you can manage"
    }

    val category: Category
        get() = when (this) {
            PAIN, SYMPTOM, DISEASE, EXTERNAL_FACTOR, PERIOD, STOOL, MOOD -> Category.OCCURRENCE
            DRUG, MEAL, SLEEP, MEDICAL_APPOINTMENT, ACTIVITY, JOURNAL, BEVERAGE, VOICE_LOGGING -> Category.MANAGEMENT
        }
}
