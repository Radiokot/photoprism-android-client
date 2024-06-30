package ua.com.radiokot.photoprism.features.importt.albums.data.model

import ua.com.radiokot.photoprism.features.gallery.search.albums.data.model.Album
import java.io.Serializable

sealed interface ImportAlbum : Serializable {
    val title: String

    data class ToCreate(
        override val title: String,
    ) : ImportAlbum

    data class Existing(
        val uid: String,
        override val title: String,
    ) : ImportAlbum {
        constructor(album: Album) : this(
            title = album.title,
            uid = album.uid,
        )
    }
}
