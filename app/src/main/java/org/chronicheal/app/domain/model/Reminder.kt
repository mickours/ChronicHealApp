package org.chronicheal.app.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import org.chronicheal.app.data.local.LocalTimeSerializer
import java.time.LocalTime

@Entity(tableName = "reminders")
@Serializable
data class Reminder(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    @Serializable(with = LocalTimeSerializer::class)
    val time: LocalTime,
    val daysOfWeek: Set<Int>, // 1 (Monday) to 7 (Sunday)
    val isEnabled: Boolean = true,
    val entryType: EntryType? = null
)
