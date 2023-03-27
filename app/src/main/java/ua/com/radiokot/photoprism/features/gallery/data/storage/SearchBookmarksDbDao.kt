package ua.com.radiokot.photoprism.features.gallery.data.storage

import androidx.room.*
import ua.com.radiokot.photoprism.features.gallery.data.model.SearchBookmarksDbEntity

@Dao
interface SearchBookmarksDbDao {
    /**
     * @return all the bookmarks ordered by position.
     */
    @Query("SELECT * FROM bookmarks ORDER BY position")
    fun getAll(): List<SearchBookmarksDbEntity>

    @Query("DELETE FROM bookmarks WHERE id=:id")
    fun delete(id: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(entity: SearchBookmarksDbEntity)

    @Update
    fun update(entity: SearchBookmarksDbEntity)

    /**
     * @return minimal (lower) position, or null if there is no bookmarks.
     */
    @Query("SELECT min(position) FROM bookmarks")
    fun getMinPosition(): Double?

    /**
     * @return next (higher) position after the bookmark with a given [id],
     * or null if the bookmark is the last one or not found.
     */
    @Query(
        "SELECT min(position) FROM bookmarks WHERE position > " +
                "(SELECT position FROM bookmarks WHERE id=:id) LIMIT 1"
    )
    fun getNextPosition(id: Long): Double?
}
