package ua.com.radiokot.photoprism.features.viewer.view.model

import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.mikepenz.fastadapter.items.AbstractItem
import ua.com.radiokot.photoprism.features.gallery.data.model.GalleryMedia
import ua.com.radiokot.photoprism.features.gallery.view.model.GalleryMediaTypeResources

sealed class MediaViewerPage(
    val thumbnailUrl: String,
    val source: GalleryMedia?,
) : AbstractItem<ViewHolder>() {

    override var identifier: Long
        get() = (thumbnailUrl + type).hashCode().toLong()
        set(_) = error("Don't override my value")

    companion object {
        fun fromGalleryMedia(source: GalleryMedia): MediaViewerPage {
            return when (source.media) {
                is GalleryMedia.TypeData.ViewableAsImage ->
                    ImageViewerPage(
                        previewUrl = source.media.hdPreviewUrl,
                        thumbnailUrl = source.smallThumbnailUrl,
                        source = source,
                    )
                is GalleryMedia.TypeData.ViewableAsVideo ->
                    VideoViewerPage(
                        previewUrl = source.media.avcPreviewUrl,
                        isLooped = source.media is GalleryMedia.TypeData.Live
                                || source.media is GalleryMedia.TypeData.Animated,
                        needsVideoControls = source.media is GalleryMedia.TypeData.Video,
                        thumbnailUrl = source.smallThumbnailUrl,
                        source = source,
                    )
                else ->
                    unsupported(source)
            }
        }

        fun unsupported(source: GalleryMedia) = UnsupportedNoticePage(
            mediaTypeIcon = GalleryMediaTypeResources.getIcon(source.media.typeName),
            mediaTypeName = GalleryMediaTypeResources.getName(source.media.typeName),
            thumbnailUrl = source.smallThumbnailUrl,
            source = source,
        )
    }
}