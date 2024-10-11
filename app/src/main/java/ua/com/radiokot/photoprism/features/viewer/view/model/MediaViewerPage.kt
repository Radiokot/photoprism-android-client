package ua.com.radiokot.photoprism.features.viewer.view.model

import android.util.Size
import com.mikepenz.fastadapter.items.AbstractItem
import ua.com.radiokot.photoprism.features.gallery.data.model.GalleryMedia
import ua.com.radiokot.photoprism.features.gallery.data.model.ViewableAsImage
import ua.com.radiokot.photoprism.features.gallery.data.model.ViewableAsVideo
import ua.com.radiokot.photoprism.features.gallery.view.model.GalleryMediaTypeResources
import ua.com.radiokot.photoprism.features.viewer.view.MediaViewerPageViewHolder
import kotlin.math.max

sealed class MediaViewerPage(
    val thumbnailUrl: String,
    val source: GalleryMedia?,
) : AbstractItem<MediaViewerPageViewHolder<out MediaViewerPage>>() {

    override var identifier: Long
        get() = (thumbnailUrl + type).hashCode().toLong()
        set(_) = error("Don't override my value")

    companion object {
        private val FADE_END_LIVE_PHOTO_KINDS = setOf(
            GalleryMedia.TypeData.Live.Kind.SAMSUNG,
            GalleryMedia.TypeData.Live.Kind.APPLE,
            GalleryMedia.TypeData.Live.Kind.GOOGLE,
        )
        private const val FADE_END_PLAYBACK_DURATION_MS_SHORT =
            400L + FadeEndLivePhotoViewerPage.FADE_DURATION_MS
        private const val FADE_END_PLAYBACK_DURATION_MS_LONG =
            1000L + FadeEndLivePhotoViewerPage.FADE_DURATION_MS

        fun fromGalleryMedia(
            source: GalleryMedia,
            imageViewSize: Size,
            livePhotosAsImages: Boolean,
        ): MediaViewerPage {
            return when {
                source.media is GalleryMedia.TypeData.Live
                        && source.media.kind in FADE_END_LIVE_PHOTO_KINDS
                        && source.media.fullDurationMs != null -> {

                    if (livePhotosAsImages) {
                        return ImageViewerPage(
                            previewUrl = source.media.getImagePreviewUrl(
                                max(
                                    imageViewSize.width,
                                    imageViewSize.height
                                )
                            ),
                            imageViewSize = imageViewSize,
                            thumbnailUrl = source.getThumbnailUrl(500),
                            source = source,
                        )
                    }

                    val videoPreviewStartMs: Long? =
                        when (source.media.kind) {
                            GalleryMedia.TypeData.Live.Kind.SAMSUNG ->
                                (source.media.fullDurationMs - FADE_END_PLAYBACK_DURATION_MS_SHORT)
                                    .coerceAtLeast(0)

                            GalleryMedia.TypeData.Live.Kind.APPLE ->
                                (source.media.fullDurationMs / 2 - FADE_END_PLAYBACK_DURATION_MS_SHORT)
                                    .coerceAtLeast(0)

                            GalleryMedia.TypeData.Live.Kind.GOOGLE ->
                                (source.media.fullDurationMs - FADE_END_PLAYBACK_DURATION_MS_LONG)
                                    .coerceAtLeast(0)

                            else ->
                                null
                        }

                    val videoPreviewEndMs: Long? =
                        when (source.media.kind) {
                            GalleryMedia.TypeData.Live.Kind.APPLE ->
                                (source.media.fullDurationMs / 2)
                                    .coerceAtLeast(0)

                            else ->
                                null
                        }

                    FadeEndLivePhotoViewerPage(
                        photoPreviewUrl = source.media.getImagePreviewUrl(
                            max(
                                imageViewSize.width,
                                imageViewSize.height
                            )
                        ),
                        videoPreviewUrl = source.media.videoPreviewUrl,
                        videoPreviewStartMs = videoPreviewStartMs,
                        videoPreviewEndMs = videoPreviewEndMs,
                        imageViewSize = imageViewSize,
                        thumbnailUrl = source.getThumbnailUrl(500),
                        source = source,
                    )
                }

                source.media is ViewableAsVideo ->
                    VideoViewerPage(
                        previewUrl = source.media.videoPreviewUrl,
                        isLooped = source.media is GalleryMedia.TypeData.Live
                                || source.media is GalleryMedia.TypeData.Animated,
                        needsVideoControls = source.media is GalleryMedia.TypeData.Video,
                        thumbnailUrl = source.getThumbnailUrl(500),
                        source = source,
                    )

                source.media is ViewableAsImage ->
                    ImageViewerPage(
                        previewUrl = source.media.getImagePreviewUrl(
                            max(
                                imageViewSize.width,
                                imageViewSize.height
                            )
                        ),
                        imageViewSize = imageViewSize,
                        thumbnailUrl = source.getThumbnailUrl(500),
                        source = source,
                    )

                else ->
                    unsupported(source)
            }
        }

        fun unsupported(source: GalleryMedia) = UnsupportedNoticePage(
            mediaTypeIcon = GalleryMediaTypeResources.getIcon(source.media.typeName),
            mediaTypeName = GalleryMediaTypeResources.getName(source.media.typeName),
            thumbnailUrl = source.getThumbnailUrl(500),
            source = source,
        )
    }
}
