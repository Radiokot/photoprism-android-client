package ua.com.radiokot.photoprism.features.importt.albums.data.model

import android.os.Parcelable
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import kotlinx.parcelize.Parcelize
import ua.com.radiokot.photoprism.features.shared.albums.data.model.Album
import java.io.Serializable

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "_t",
)
@JsonSubTypes(
    JsonSubTypes.Type(value = ImportAlbum.ToCreate::class, name = "ToCreate"),
    JsonSubTypes.Type(value = ImportAlbum.Existing::class, name = "Existing"),
)
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
