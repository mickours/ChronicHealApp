package org.chronicheal.app.data.local

import androidx.room.TypeConverter
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.chronicheal.app.domain.model.EntryType
import java.time.Instant
import java.time.LocalTime

class Converters {
    @TypeConverter
    fun fromTimestamp(value: Long?): Instant? {
        return value?.let { Instant.ofEpochMilli(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Instant?): Long? {
        return date?.toEpochMilli()
    }

    @TypeConverter
    fun fromEntryType(value: EntryType?): String? {
        return value?.name
    }

    @TypeConverter
    fun toEntryType(value: String?): EntryType? {
        return value?.let {
            try {
                EntryType.valueOf(it)
            } catch (e: IllegalArgumentException) {
                null
            }
        }
    }

    @TypeConverter
    fun fromLocalTime(value: LocalTime?): String? {
        return value?.toString()
    }

    @TypeConverter
    fun toLocalTime(value: String?): LocalTime? {
        return value?.let { LocalTime.parse(it) }
    }

    @TypeConverter
    fun fromIntSet(value: Set<Int>?): String? {
        return value?.let { Json.encodeToString(it) }
    }

    @TypeConverter
    fun toIntSet(value: String?): Set<Int>? {
        return value?.let { Json.decodeFromString(it) }
    }
}
