package ua.com.radiokot.photoprism.features.gallery.data.storage

import androidx.room.*
import ua.com.radiokot.photoprism.features.gallery.data.model.SearchBookmarksDbEntity

@Dao
interface SearchBookmarksDbDao {
    @Query("SELECT * FROM bookmarks ORDER BY id DESC")
    fun getAll(): List<SearchBookmarksDbEntity>

    @Query("DELETE FROM bookmarks WHERE id=:id")
    fun delete(id: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(entity: SearchBookmarksDbEntity)

    @Update
    fun update(entity: SearchBookmarksDbEntity)
}
