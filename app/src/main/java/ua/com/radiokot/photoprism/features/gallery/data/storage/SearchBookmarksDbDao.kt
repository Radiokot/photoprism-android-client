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

    @Query("DELETE FROM bookmarks")
    fun deleteAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(vararg entities: SearchBookmarksDbEntity)

    @Update
    fun update(entity: SearchBookmarksDbEntity)

    /**
     * @return minimal (lower) position, or null if there is no bookmarks.
     */
    @Query("SELECT min(position) FROM bookmarks")
    fun getMinPosition(): Double?

    /**
     * @return current and next (higher) position after the bookmark with a given [id].
     * If the bookmark is last, only its position is returned.
     * If the bookmark is not found, nothing is returned.
     *
     */
    @Query(
        "SELECT position FROM bookmarks WHERE position >= " +
                "(SELECT position FROM bookmarks WHERE id=:id) ORDER BY position LIMIT 2"
    )
    fun getIdPositionAndNext(id: Long): List<Double>
}
