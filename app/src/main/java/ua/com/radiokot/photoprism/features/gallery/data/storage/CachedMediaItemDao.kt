package ua.com.radiokot.photoprism.features.gallery.data.storage

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import ua.com.radiokot.photoprism.features.gallery.data.model.CachedMediaItemRecord

@Dao
interface CachedMediaItemDao {
    @Insert(onConflict = OnConflictStrategy.Companion.REPLACE)
    fun insert(item: CachedMediaItemRecord): Completable

    @Query("SELECT sum(size) FROM cached_media_items")
    fun getCacheSizeInBytes(): Long

    @Query("SELECT * FROM cached_media_items ORDER BY lastHitAt ASC")
    fun getAllByLastHit(): List<CachedMediaItemRecord>

    @Query("SELECT * FROM cached_media_items WHERE `hash` = :hash")
    fun getByHash(hash: String): CachedMediaItemRecord?

    @Query("UPDATE cached_media_items SET lastHitAt = :timestamp WHERE `hash` = :hash")
    fun updateLastHit(hash: String, timestamp: Long): Completable

    @Query("DELETE FROM cached_media_items WHERE `hash` = :hash")
    fun deleteByHash(hash: String): Completable

    @Query("DELETE FROM cached_media_items WHERE 'id' = :id")
    fun deleteById(id: Long): Completable
}