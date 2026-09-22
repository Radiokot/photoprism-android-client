package ua.com.radiokot.photoprism.features.gallery.view.model

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import ua.com.radiokot.photoprism.R
import ua.com.radiokot.photoprism.features.gallery.data.model.GalleryMedia
import ua.com.radiokot.photoprism.features.gallery.data.model.Viewable

object GalleryMediaResources {

    @DrawableRes
    val regularImageIcon = R.drawable.ic_image

    @DrawableRes
    fun getIcon(source: GalleryMedia): Int = when {

        // Special icon for 360 panorama.
        source.media is Viewable.AsImage
                && source.panoramaProjection != null ->
            R.drawable.ic_panorama

        else ->
            getTypeIcon(source.media.typeName)
    }

    @DrawableRes
    fun getTypeIcon(typeName: GalleryMedia.TypeName): Int = when (typeName) {
        GalleryMedia.TypeName.IMAGE ->
            regularImageIcon

        GalleryMedia.TypeName.LIVE ->
            R.drawable.ic_live_photo

        GalleryMedia.TypeName.VECTOR ->
            R.drawable.ic_vector

        GalleryMedia.TypeName.VIDEO ->
            R.drawable.ic_video

        GalleryMedia.TypeName.RAW ->
            R.drawable.ic_shutter

        GalleryMedia.TypeName.ANIMATED ->
            R.drawable.ic_animation

        else ->
            R.drawable.ic_file
    }

    @StringRes
    fun getTypeName(typeName: GalleryMedia.TypeName): Int = when (typeName) {
        GalleryMedia.TypeName.IMAGE ->
            R.string.media_type_image

        GalleryMedia.TypeName.ANIMATED ->
            R.string.media_type_animated

        GalleryMedia.TypeName.LIVE ->
            R.string.media_type_live

        GalleryMedia.TypeName.OTHER ->
            R.string.media_type_other

        GalleryMedia.TypeName.RAW ->
            R.string.media_type_raw

        GalleryMedia.TypeName.SIDECAR ->
            R.string.media_type_sidecar

        GalleryMedia.TypeName.TEXT ->
            R.string.media_type_text

        GalleryMedia.TypeName.UNKNOWN ->
            R.string.media_type_unknown

        GalleryMedia.TypeName.VECTOR ->
            R.string.media_type_vector

        GalleryMedia.TypeName.VIDEO ->
            R.string.media_type_video
    }
}
