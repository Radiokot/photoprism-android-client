package ua.com.radiokot.photoprism.features.ext.memories.data.storage

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import ua.com.radiokot.photoprism.features.ext.memories.data.model.MemoryDbEntity

@Dao
interface MemoriesDbDao {
    @Query("SELECT * FROM memories")
    fun getAll(): List<MemoryDbEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(entities: Collection<MemoryDbEntity>)

    @Query("DELETE FROM memories WHERE created_at_ms<:maxCreatedAtMs")
    fun deleteExpired(maxCreatedAtMs: Long): Int

    @Query("DELETE FROM memories")
    fun deleteAll()

    @Query("DELETE FROM memories WHERE searchQuery=:memorySearchQuery")
    fun delete(memorySearchQuery: String)

    @Query("UPDATE memories SET is_seen=1 WHERE searchQuery=:memorySearchQuery")
    fun markAsSeen(memorySearchQuery: String)
}
