package ua.com.radiokot.photoprism.features.gallery.view.model

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import ua.com.radiokot.photoprism.R
import ua.com.radiokot.photoprism.features.gallery.data.model.GalleryMedia

object GalleryMediaTypeResources {
    @DrawableRes
    fun getIcon(typeName: GalleryMedia.TypeName): Int = when (typeName) {
        GalleryMedia.TypeName.IMAGE ->
            R.drawable.ic_image
        GalleryMedia.TypeName.ANIMATED ->
            R.drawable.ic_animation
        GalleryMedia.TypeName.LIVE ->
            R.drawable.ic_live_photo
        GalleryMedia.TypeName.OTHER ->
            R.drawable.ic_sledding
        GalleryMedia.TypeName.RAW ->
            R.drawable.ic_raw
        GalleryMedia.TypeName.SIDECAR ->
            R.drawable.ic_attachment
        GalleryMedia.TypeName.TEXT ->
            R.drawable.ic_text
        GalleryMedia.TypeName.UNKNOWN ->
            R.drawable.ic_unknown
        GalleryMedia.TypeName.VECTOR ->
            R.drawable.ic_curve
        GalleryMedia.TypeName.VIDEO ->
            R.drawable.ic_video
    }

    @StringRes
    fun getName(typeName: GalleryMedia.TypeName): Int = when (typeName) {
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