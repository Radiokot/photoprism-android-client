package ua.com.radiokot.photoprism.features.gallery.data.model

import ua.com.radiokot.photoprism.api.photos.model.PhotoPrismPhoto
import java.util.Date

sealed class GalleryMedia(
    val hash: String,
    val width: Int,
    val height: Int,
    val takenAt: Date,
) {
    class Photo(hash: String, width: Int, height: Int, takenAt: Date) :
        GalleryMedia(hash, width, height, takenAt) {
        constructor(source: PhotoPrismPhoto) : this(
            hash = source.hash,
            width = source.width,
            height = source.height,
            takenAt = photoPrismDateFormat.parse(source.takenAt)!!
        )
    }

    class Video(hash: String, width: Int, height: Int, takenAt: Date) :
        GalleryMedia(hash, width, height, takenAt) {
        constructor(source: PhotoPrismPhoto) : this(
            hash = source.hash,
            width = source.width,
            height = source.height,
            takenAt = photoPrismDateFormat.parse(source.takenAt)!!
        )
    }

    companion object {
        fun fromPhotoPrism(source: PhotoPrismPhoto): GalleryMedia {
            return when (val type = source.type) {
                "image" ->
                    Photo(source)
                "video" ->
                    Video(source)
                else ->
                    throw IllegalStateException("Unsupported PhotoPrism photo type '$type'")
            }
        }
    }
}