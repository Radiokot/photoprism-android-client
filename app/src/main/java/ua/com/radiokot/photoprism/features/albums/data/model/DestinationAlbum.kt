package ua.com.radiokot.photoprism.features.albums.data.model

import android.os.Parcelable
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import kotlinx.parcelize.Parcelize
import java.io.Serializable

/**
 * An album to add media into. Can be either existing or to be created.
 */
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "_t",
)
@JsonSubTypes(
    JsonSubTypes.Type(value = DestinationAlbum.ToCreate::class, name = "ToCreate"),
    JsonSubTypes.Type(value = DestinationAlbum.Existing::class, name = "Existing"),
)
sealed interface DestinationAlbum : Parcelable, Serializable {
    val title: String

    @Parcelize
    data class ToCreate(
        override val title: String,
    ) : DestinationAlbum

    @Parcelize
    data class Existing(
        val uid: String,
        override val title: String,
    ) : DestinationAlbum {
        constructor(album: Album) : this(
            title = album.title,
            uid = album.uid,
        )
    }
}
