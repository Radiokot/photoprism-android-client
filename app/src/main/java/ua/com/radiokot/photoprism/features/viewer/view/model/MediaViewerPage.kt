package ua.com.radiokot.photoprism.features.viewer.view.model

import android.util.Size
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.mikepenz.fastadapter.items.AbstractItem
import ua.com.radiokot.photoprism.features.gallery.data.model.GalleryMedia
import ua.com.radiokot.photoprism.features.gallery.view.model.GalleryMediaTypeResources
import kotlin.math.max

sealed class MediaViewerPage(
    val thumbnailUrl: String,
    val source: GalleryMedia?,
) : AbstractItem<ViewHolder>() {

    override var identifier: Long
        get() = (thumbnailUrl + type).hashCode().toLong()
        set(_) = error("Don't override my value")

    companion object {
        fun fromGalleryMedia(
            source: GalleryMedia,
            imageViewSize: Size,
        ): MediaViewerPage {
            return when (source.media) {
                is GalleryMedia.TypeData.Live ->
                    LivePhotoViewerPage(
                        photoPreviewUrl = source.media.getPreviewUrl(
                            max(
                                imageViewSize.width,
                                imageViewSize.height
                            )
                        ),
                        videoPreviewUrl = source.media.avcPreviewUrl,
                        fullVideoDurationMs = source.media.fullDurationMs,
                        imageViewSize = imageViewSize,
                        thumbnailUrl = source.smallThumbnailUrl,
                        source = source,
                    )

                is GalleryMedia.TypeData.ViewableAsImage ->
                    ImageViewerPage(
                        previewUrl = source.media.getPreviewUrl(
                            max(
                                imageViewSize.width,
                                imageViewSize.height
                            )
                        ),
                        imageViewSize = imageViewSize,
                        thumbnailUrl = source.smallThumbnailUrl,
                        source = source,
                    )

                is GalleryMedia.TypeData.ViewableAsVideo ->
                    VideoViewerPage(
                        previewUrl = source.media.avcPreviewUrl,
                        isLooped = source.media is GalleryMedia.TypeData.Animated,
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
