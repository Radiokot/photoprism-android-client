package ua.com.radiokot.photoprism.features.viewer.view.model

import android.util.Size
import com.mikepenz.fastadapter.items.AbstractItem
import ua.com.radiokot.photoprism.features.gallery.data.model.GalleryMedia
import ua.com.radiokot.photoprism.features.gallery.data.model.Viewable
import ua.com.radiokot.photoprism.features.gallery.logic.MediaPreviewUrlFactory
import ua.com.radiokot.photoprism.features.gallery.view.model.GalleryMediaTypeResources
import ua.com.radiokot.photoprism.features.viewer.view.MediaViewerPageViewHolder
import kotlin.math.max

// MediaViewerPageDiffCallback must be updated when adding new page types.
sealed class MediaViewerPage(
    val thumbnailUrl: String,
    val source: GalleryMedia?,
) : AbstractItem<MediaViewerPageViewHolder<out MediaViewerPage>>() {

    // Used in MediaViewerPageDiffCallback for item equality.
    override var identifier: Long
        get() = (thumbnailUrl + type).hashCode().toLong()
        set(_) = error("Don't override my value")

    companion object {
        private const val FADE_END_PLAYBACK_DURATION_MS_SHORT =
            400L + FadeEndLivePhotoViewerPage.FADE_DURATION_MS
        private const val FADE_END_PLAYBACK_DURATION_MS_LONG =
            1000L + FadeEndLivePhotoViewerPage.FADE_DURATION_MS
        private const val THUMBNAIL_SIZE_PX = 500

        fun fromGalleryMedia(
            source: GalleryMedia,
            imageViewSize: Size,
            livePhotosAsImages: Boolean,
            previewUrlFactory: MediaPreviewUrlFactory,
        ): MediaViewerPage {
            return when {
                source.media is GalleryMedia.TypeData.Live
                        && source.media.fullDurationMs != null -> {

                    if (livePhotosAsImages) {
                        return ImageViewerPage(
                            previewUrl = previewUrlFactory.getImagePreviewUrl(
                                previewHash = source.hash,
                                sizePx = max(
                                    imageViewSize.width,
                                    imageViewSize.height
                                )
                            ),
                            imageViewSize = imageViewSize,
                            thumbnailUrl = previewUrlFactory.getThumbnailUrl(
                                thumbnailHash = source.hash,
                                sizePx = THUMBNAIL_SIZE_PX,
                            ),
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
                        photoPreviewUrl = previewUrlFactory.getImagePreviewUrl(
                            previewHash = source.hash,
                            sizePx = max(
                                imageViewSize.width,
                                imageViewSize.height
                            )
                        ),
                        videoPreviewUrl = previewUrlFactory.getVideoPreviewUrl(
                            galleryMedia = source,
                        ),
                        videoPreviewStartMs = videoPreviewStartMs,
                        videoPreviewEndMs = videoPreviewEndMs,
                        imageViewSize = imageViewSize,
                        thumbnailUrl = previewUrlFactory.getThumbnailUrl(
                            thumbnailHash = source.hash,
                            sizePx = THUMBNAIL_SIZE_PX,
                        ),
                        source = source,
                    )
                }

                source.media is Viewable.AsVideo ->
                    VideoViewerPage(
                        previewUrl = previewUrlFactory.getVideoPreviewUrl(
                            galleryMedia = source,
                        ),
                        isLooped = source.media is GalleryMedia.TypeData.Live
                                || source.media is GalleryMedia.TypeData.Animated,
                        needsVideoControls = source.media is GalleryMedia.TypeData.Video,
                        thumbnailUrl = previewUrlFactory.getThumbnailUrl(
                            thumbnailHash = source.hash,
                            sizePx = THUMBNAIL_SIZE_PX,
                        ),
                        source = source,
                    )

                source.media is Viewable.AsImage ->
                    ImageViewerPage(
                        previewUrl = source.files.first().cachedPath
                            ?: previewUrlFactory.getImagePreviewUrl(
                                previewHash = source.hash,
                                sizePx = max(
                                    imageViewSize.width,
                                    imageViewSize.height
                                )
                            ),
                        imageViewSize = imageViewSize,
                        thumbnailUrl = previewUrlFactory.getThumbnailUrl(
                            thumbnailHash = source.hash,
                            sizePx = THUMBNAIL_SIZE_PX,
                        ),
                        source = source,
                    )

                else ->
                    unsupported(source, previewUrlFactory)
            }
        }

        fun unsupported(
            source: GalleryMedia,
            previewUrlFactory: MediaPreviewUrlFactory,
        ) = UnsupportedNoticePage(
            mediaTypeIcon = GalleryMediaTypeResources.getIcon(source.media.typeName),
            mediaTypeName = GalleryMediaTypeResources.getName(source.media.typeName),
            thumbnailUrl = previewUrlFactory.getThumbnailUrl(
                thumbnailHash = source.hash,
                sizePx = THUMBNAIL_SIZE_PX,
            ),
            source = source,
        )
    }
}
