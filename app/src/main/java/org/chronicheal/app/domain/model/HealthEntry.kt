package org.chronicheal.app.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import org.chronicheal.app.data.local.InstantSerializer
import java.time.Instant

@Entity(tableName = "health_entries")
@Serializable
data class HealthEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @Serializable(with = InstantSerializer::class)
    val timestamp: Instant = Instant.now(),
    val type: EntryType,
    val note: String = "",
    val intensity: Int? = null,
    val location: String? = null,
    val name: String? = null,
    val value: Double? = null,
    val unit: String? = null,
    val hasReminder: Boolean = false,
    val reminderId: Long? = null,
    val durationMinutes: Int? = null
)
