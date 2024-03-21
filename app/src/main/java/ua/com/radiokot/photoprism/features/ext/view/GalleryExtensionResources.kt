package ua.com.radiokot.photoprism.features.ext.view

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import ua.com.radiokot.photoprism.R
import ua.com.radiokot.photoprism.features.ext.data.model.GalleryExtension

object GalleryExtensionResources {
    @DrawableRes
    fun getIcon(extension: GalleryExtension): Int = when (extension) {
        GalleryExtension.MEMORIES ->
            R.drawable.ic_photo_album_white

        GalleryExtension.TEST ->
            R.drawable.ic_sledding
    }

    @StringRes
    fun getTitle(extension: GalleryExtension): Int = when (extension) {
        GalleryExtension.MEMORIES ->
            R.string.extension_memories_title

        GalleryExtension.TEST ->
            R.string.media_type_unknown
    }

    @StringRes
    fun getDescription(extension: GalleryExtension): Int = when (extension) {
        GalleryExtension.MEMORIES ->
            R.string.extension_memories_description

        GalleryExtension.TEST ->
            R.string.media_type_unknown
    }

    fun getBannerUrl(extension: GalleryExtension):String=when(extension){
        GalleryExtension.MEMORIES ->
            "https://feed.radiokot.com.ua/thumb/architecture.jpg"

        GalleryExtension.TEST ->
            "https://img3.teletype.in/files/6e/f5/6ef5b234-528c-400f-8ac7-bb00da2ce2c9.jpeg"
    }
}
