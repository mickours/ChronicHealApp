package org.chronicheal.app.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(tableName = "health_entries")
data class HealthEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Instant = Instant.now(),
    val type: EntryType,
    val note: String = "",
    val intensity: Int? = null,
    val location: String? = null,
    val name: String? = null,
    val value: Double? = null,
    val unit: String? = null
)
