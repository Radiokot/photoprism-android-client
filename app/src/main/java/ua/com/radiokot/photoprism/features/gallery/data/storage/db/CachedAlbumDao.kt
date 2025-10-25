package ua.com.radiokot.photoprism.features.gallery.data.storage.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable

@Dao
interface CachedAlbumDao {
    @Query("SELECT * FROM cached_albums")
    fun getCachedAlbums(): Observable<List<CachedAlbum>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun addCachedAlbum(album: CachedAlbum): Completable

    @Query("DELETE FROM cached_albums WHERE albumId = :albumId")
    fun deleteCachedAlbum(albumId: String): Completable

    @Query("SELECT EXISTS(SELECT 1 FROM cached_albums WHERE albumId = :albumId)")
    fun isAlbumCached(albumId: String): Observable<Boolean>
}
