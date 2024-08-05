package ua.com.radiokot.photoprism.features.ext.view

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import ua.com.radiokot.photoprism.R
import ua.com.radiokot.photoprism.features.ext.data.model.GalleryExtension

object GalleryExtensionResources {
    @StringRes
    fun getTitle(extension: GalleryExtension): Int = when (extension) {
        GalleryExtension.MEMORIES ->
            R.string.extension_memories_title

        GalleryExtension.TEST ->
            R.string.media_type_unknown

        GalleryExtension.PHOTO_FRAME_WIDGET ->
            R.string.extension_photo_frame_widget_name
    }

    @StringRes
    fun getDescription(extension: GalleryExtension): Int = when (extension) {
        GalleryExtension.MEMORIES ->
            R.string.extension_memories_description

        GalleryExtension.TEST ->
            R.string.media_type_unknown

        GalleryExtension.PHOTO_FRAME_WIDGET ->
            R.string.extension_photo_frame_widget_description
    }

    @DrawableRes
    fun getBanner(extension: GalleryExtension): Int = when (extension) {
        GalleryExtension.MEMORIES ->
            R.drawable.banner_memories

        GalleryExtension.TEST ->
            R.drawable.tv_banner

        // TODO Add a proper image
        GalleryExtension.PHOTO_FRAME_WIDGET ->
            R.drawable.sample_image
    }
}
