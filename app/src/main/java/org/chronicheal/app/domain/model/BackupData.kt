package org.chronicheal.app.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class BackupData(
    val schemaVersion: Int,
    val entries: List<HealthEntry>,
    val exportTimestamp: Long = System.currentTimeMillis()
)
