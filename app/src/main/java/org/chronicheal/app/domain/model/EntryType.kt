package org.chronicheal.app.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class EntryType {
    PAIN,
    DRUG,
    SYMPTOM,
    DISEASE,
    MEAL,
    SLEEP,
    MEDICAL_APPOINTMENT,
    ACTIVITY,
    EXTERNAL_FACTOR,
    JOURNAL
}
