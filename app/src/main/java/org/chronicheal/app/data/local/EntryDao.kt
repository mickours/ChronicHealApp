package org.chronicheal.app.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import org.chronicheal.app.domain.model.HealthEntry

@Dao
interface EntryDao {
    @Query("SELECT * FROM health_entries ORDER BY timestamp DESC")
    fun getAllEntries(): Flow<List<HealthEntry>>

    @Query("SELECT * FROM health_entries WHERE id = :id")
    suspend fun getEntryById(id: Long): HealthEntry?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: HealthEntry)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntries(entries: List<HealthEntry>)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateEntry(entry: HealthEntry)

    @Delete
    suspend fun deleteEntry(entry: HealthEntry)
}
