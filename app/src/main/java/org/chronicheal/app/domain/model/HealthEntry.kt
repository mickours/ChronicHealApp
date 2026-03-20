package org.chronicheal.app.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import org.chronicheal.app.data.local.InstantSerializer
import java.time.Instant

@Serializable
data class Ingredient(
    val name: String,
    val quantity: Double? = null,
    val unit: String? = null
)

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
    val origin: String? = null,
    val value: Double? = null,
    val unit: String? = null,
    val hasReminder: Boolean = false,
    val reminderId: Long? = null,
    val durationMinutes: Int? = null,
    val isFinished: Boolean = false,
    val ingredients: List<Ingredient>? = null,
    val isAlcoholic: Boolean? = null,
    val isCaffeinated: Boolean? = null,
    val allergens: List<String>? = null,
    val fodmaps: List<String>? = null,
    val proteins: Double? = null,
    val carbohydrates: Double? = null,
    val lipids: Double? = null
)
