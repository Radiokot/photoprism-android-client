package ua.com.radiokot.photoprism.features.gallery.data.model

import ua.com.radiokot.photoprism.api.photos.model.PhotoPrismPhoto
import ua.com.radiokot.photoprism.features.gallery.logic.MediaThumbnailUrlFactory
import java.util.*

sealed class GalleryMedia(
    val hash: String,
    val width: Int,
    val height: Int,
    val takenAt: Date,
    val name: String,
    val smallThumbnailUrl: String,
) {
    class Photo(
        hash: String,
        width: Int,
        height: Int,
        takenAt: Date,
        name: String,
        smallThumbnailUrl: String,
    ) : GalleryMedia(hash, width, height, takenAt, name, smallThumbnailUrl) {
        constructor(
            source: PhotoPrismPhoto,
            smallThumbnailUrl: String,
        ) : this(
            hash = source.hash,
            width = source.width,
            height = source.height,
            takenAt = photoPrismDateFormat.parse(source.takenAt)!!,
            name = source.name,
            smallThumbnailUrl = smallThumbnailUrl,
        )

        override fun toString(): String {
            return "Photo(hash='$hash')"
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Photo || hash != other.hash) return false
            return true
        }

        override fun hashCode(): Int {
            return hash.hashCode()
        }
    }

    class Video(
        hash: String,
        width: Int,
        height: Int,
        takenAt: Date,
        name: String,
        smallThumbnailUrl: String,
    ) : GalleryMedia(hash, width, height, takenAt, name, smallThumbnailUrl) {
        constructor(
            source: PhotoPrismPhoto,
            smallThumbnailUrl: String,
        ) : this(
            hash = source.hash,
            width = source.width,
            height = source.height,
            takenAt = photoPrismDateFormat.parse(source.takenAt)!!,
            name = source.name,
            smallThumbnailUrl = smallThumbnailUrl,
        )

        override fun toString(): String {
            return "Video(hash='$hash')"
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Video || hash != other.hash) return false
            return true
        }

        override fun hashCode(): Int {
            return hash.hashCode()
        }
    }

    companion object {
        fun fromPhotoPrism(
            source: PhotoPrismPhoto,
            thumbnailUrlFactory: MediaThumbnailUrlFactory,
        ): GalleryMedia {
            return when (val type = source.type) {
                "image" ->
                    Photo(
                        source = source,
                        smallThumbnailUrl = thumbnailUrlFactory.getSmallThumbnailUrl(source.hash),
                    )
                "video" ->
                    Video(
                        source,
                        smallThumbnailUrl = thumbnailUrlFactory.getSmallThumbnailUrl(source.hash),
                    )
                else ->
                    throw IllegalStateException("Unsupported PhotoPrism photo type '$type'")
            }
        }
    }
}