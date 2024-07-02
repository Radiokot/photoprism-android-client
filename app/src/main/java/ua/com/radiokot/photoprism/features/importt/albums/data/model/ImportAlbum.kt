package ua.com.radiokot.photoprism.features.importt.albums.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import ua.com.radiokot.photoprism.features.gallery.search.albums.data.model.Album
import java.io.Serializable

sealed interface ImportAlbum : Parcelable, Serializable {
    val title: String

    @Parcelize
    data class ToCreate(
        override val title: String,
    ) : ImportAlbum

    @Parcelize
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
