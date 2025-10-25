package ua.com.radiokot.photoprism.features.gallery.data.storage.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        CachedAlbum::class
    ],
    version = 1,
    exportSchema = false
)
abstract class GalleryDatabase : RoomDatabase() {
    abstract fun getCachedAlbumDao(): CachedAlbumDao
}
