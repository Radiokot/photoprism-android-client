package ua.com.radiokot.photoprism.features.albums.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cached_albums")
data class CachedAlbum(
    @PrimaryKey
    val albumId: String,
) {
    companion object {
        const val CACHED_PHOTOS_ALBUM_ID = "- favourites -"
    }
}