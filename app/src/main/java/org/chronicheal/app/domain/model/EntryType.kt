package org.chronicheal.app.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class EntryType(val emoji: String) {
    PAIN("💥"),
    DRUG("💊"),
    SYMPTOM("🌡️"),
    DISEASE("🏥"),
    MEAL("🍲"),
    SLEEP("😴"),
    MEDICAL_APPOINTMENT("👨‍⚕️"),
    ACTIVITY("🏃"),
    EXTERNAL_FACTOR("☁️"),
    JOURNAL("📝"),
    PERIOD("🩸");

    enum class Category {
        OCCURRENCE, // "What occurs to you"
        MANAGEMENT  // "What you can manage"
    }

    val category: Category
        get() = when (this) {
            PAIN, SYMPTOM, DISEASE, EXTERNAL_FACTOR, PERIOD -> Category.OCCURRENCE
            DRUG, MEAL, SLEEP, MEDICAL_APPOINTMENT, ACTIVITY, JOURNAL -> Category.MANAGEMENT
        }
}
