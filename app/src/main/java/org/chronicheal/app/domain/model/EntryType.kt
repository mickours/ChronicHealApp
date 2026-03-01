package org.chronicheal.app.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class EntryType(val defaultDurationMinutes: Int, val emoji: String) {
    PAIN(15, "💥"),
    DRUG(0, "💊"),
    SYMPTOM(30, "🌡️"),
    DISEASE(1440, "🏥"),
    MEAL(30, "🍲"),
    SLEEP(480, "😴"),
    MEDICAL_APPOINTMENT(60, "👨‍⚕️"),
    ACTIVITY(30, "🏃"),
    EXTERNAL_FACTOR(0, "☁️"),
    JOURNAL(10, "📝");

    enum class Category {
        OCCURRENCE, // "What occurs to you"
        MANAGEMENT  // "What you can manage"
    }

    val category: Category
        get() = when (this) {
            PAIN, SYMPTOM, DISEASE, EXTERNAL_FACTOR -> Category.OCCURRENCE
            DRUG, MEAL, SLEEP, MEDICAL_APPOINTMENT, ACTIVITY, JOURNAL -> Category.MANAGEMENT
        }
}
