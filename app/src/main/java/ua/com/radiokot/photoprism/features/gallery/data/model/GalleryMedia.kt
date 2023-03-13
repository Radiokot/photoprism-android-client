package ua.com.radiokot.photoprism.features.gallery.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import ua.com.radiokot.photoprism.api.photos.model.PhotoPrismPhoto
import ua.com.radiokot.photoprism.features.gallery.logic.MediaFileDownloadUrlFactory
import ua.com.radiokot.photoprism.features.gallery.logic.MediaThumbnailUrlFactory
import java.util.*

class GalleryMedia(
    val media: MediaType,
    val hash: String,
    val width: Int,
    val height: Int,
    val takenAt: Date,
    val name: String,
    val smallThumbnailUrl: String,
    val files: List<File>,
) {
    constructor(
        source: PhotoPrismPhoto,
        thumbnailUrlFactory: MediaThumbnailUrlFactory,
        downloadUrlFactory: MediaFileDownloadUrlFactory,
    ) : this(
        media = MediaType.fromPhotoPrism(source.type),
        hash = source.hash,
        width = source.width,
        height = source.height,
        takenAt = photoPrismDateFormat.parse(source.takenAt)!!,
        name = source.name,
        smallThumbnailUrl = thumbnailUrlFactory.getSmallThumbnailUrl(source.hash),
        files = source.files.map { photoPrismFile ->
            File(
                source = photoPrismFile,
                thumbnailUrlFactory = thumbnailUrlFactory,
                downloadUrlFactory = downloadUrlFactory,
            )
        }
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as GalleryMedia

        if (hash != other.hash) return false

        return true
    }

    override fun hashCode(): Int {
        return hash.hashCode()
    }

    override fun toString(): String {
        return "GalleryMedia(hash='$hash', kind=$media)"
    }


    /**
    photoprism/pkg/media/types.go

    const (
    Unknown  Type = ""
    Image    Type = "image"
    Raw      Type = "raw"
    Animated Type = "animated"
    Live     Type = "live"
    Video    Type = "video"
    Vector   Type = "vector"
    Sidecar  Type = "sidecar"
    Text     Type = "text"
    Other    Type = "other"
    )
     */
    sealed class MediaType(val value: String) {
        object Unknown : MediaType("")
        object Image : MediaType("image")
        object Raw : MediaType("video")
        object Animated : MediaType("animated")
        object Live : MediaType("live")
        object Video : MediaType("video")
        object Vector : MediaType("vector")
        object Sidecar : MediaType("sidecar")
        object Text : MediaType("text")
        object Other : MediaType("other")

        companion object {
            fun fromPhotoPrism(type: String): MediaType =
                when (type) {
                    "" -> Unknown
                    "image" -> Image
                    "raw" -> Raw
                    "animated" -> Animated
                    "live" -> Live
                    "video" -> Video
                    "vector" -> Vector
                    "sidecar" -> Sidecar
                    "text" -> Text
                    "other" -> Other
                    else ->
                        throw IllegalStateException("Unsupported PhotoPrism media type '$type'")
                }
        }
    }

    @Parcelize
    class File(
        val hash: String,
        val name: String,
        val mimeType: String,
        val sizeBytes: Long,
        val thumbnailUrlSmall: String,
        val downloadUrl: String,
    ) : Parcelable {
        constructor(
            source: PhotoPrismPhoto.File,
            thumbnailUrlFactory: MediaThumbnailUrlFactory,
            downloadUrlFactory: MediaFileDownloadUrlFactory,
        ) : this(
            hash = source.hash,
            name = source.name,
            mimeType = source.mime,
            sizeBytes = source.size,
            thumbnailUrlSmall = thumbnailUrlFactory.getSmallThumbnailUrl(source.hash),
            downloadUrl = downloadUrlFactory.getDownloadUrl(source.hash),
        )
    }
}