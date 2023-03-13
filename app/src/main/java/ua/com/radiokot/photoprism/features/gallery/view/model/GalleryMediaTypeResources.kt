package ua.com.radiokot.photoprism.features.gallery.view.model

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import ua.com.radiokot.photoprism.R
import ua.com.radiokot.photoprism.features.gallery.data.model.GalleryMedia

object GalleryMediaTypeResources {
    @DrawableRes
    fun getIcon(type: GalleryMedia.MediaType): Int? = when (type) {
        GalleryMedia.MediaType.Image ->
            null
        GalleryMedia.MediaType.Animated ->
            R.drawable.ic_animation
        GalleryMedia.MediaType.Live ->
            R.drawable.ic_live_photo
        GalleryMedia.MediaType.Other ->
            R.drawable.ic_sledding
        GalleryMedia.MediaType.Raw ->
            R.drawable.ic_raw
        GalleryMedia.MediaType.Sidecar ->
            R.drawable.ic_attachment
        GalleryMedia.MediaType.Text ->
            R.drawable.ic_text
        GalleryMedia.MediaType.Unknown ->
            R.drawable.ic_unknown
        GalleryMedia.MediaType.Vector ->
            R.drawable.ic_curve
        GalleryMedia.MediaType.Video ->
            R.drawable.ic_video
    }

    @StringRes
    fun getName(type: GalleryMedia.MediaType): Int = when (type) {
        GalleryMedia.MediaType.Image ->
            R.string.media_type_image
        GalleryMedia.MediaType.Animated ->
            R.string.media_type_animated
        GalleryMedia.MediaType.Live ->
            R.string.media_type_live
        GalleryMedia.MediaType.Other ->
            R.string.media_type_other
        GalleryMedia.MediaType.Raw ->
            R.string.media_type_raw
        GalleryMedia.MediaType.Sidecar ->
            R.string.media_type_sidecar
        GalleryMedia.MediaType.Text ->
            R.string.media_type_text
        GalleryMedia.MediaType.Unknown ->
            R.string.media_type_unknown
        GalleryMedia.MediaType.Vector ->
            R.string.media_type_vector
        GalleryMedia.MediaType.Video ->
            R.string.media_type_video
    }
}