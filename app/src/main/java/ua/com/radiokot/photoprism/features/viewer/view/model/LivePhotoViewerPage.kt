package ua.com.radiokot.photoprism.features.viewer.view.model

import android.net.Uri
import android.util.Size
import android.view.View
import androidx.core.view.isVisible
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.util.MimeTypes
import com.mikepenz.fastadapter.FastAdapter
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import org.koin.core.component.KoinScopeComponent
import org.koin.core.component.inject
import org.koin.core.scope.Scope
import ua.com.radiokot.photoprism.R
import ua.com.radiokot.photoprism.databinding.PagerItemMediaViewerLivePhotoBinding
import ua.com.radiokot.photoprism.di.DI_SCOPE_SESSION
import ua.com.radiokot.photoprism.extension.fadeIn
import ua.com.radiokot.photoprism.extension.hardwareConfigIfAvailable
import ua.com.radiokot.photoprism.features.gallery.data.model.GalleryMedia
import ua.com.radiokot.photoprism.features.viewer.view.VideoPlayerViewHolder
import ua.com.radiokot.photoprism.features.viewer.view.VideoPlayerViewHolderImpl

class LivePhotoViewerPage(
    videoPreviewUrl: String,
    val fullVideoDurationMs: Long?,
    val photoPreviewUrl: String,
    val imageViewSize: Size,
    thumbnailUrl: String,
    source: GalleryMedia?,
) : MediaViewerPage(thumbnailUrl, source) {
    val videoPreviewUri: Uri = Uri.parse(videoPreviewUrl)
    val mediaId: String = identifier.toString()

    override val type: Int
        get() = R.id.pager_item_media_viewer_live_photo

    override val layoutRes: Int
        get() = R.layout.pager_item_media_viewer_live_photo

    override fun getViewHolder(v: View): ViewHolder =
        ViewHolder(PagerItemMediaViewerLivePhotoBinding.bind(v))

    class ViewHolder(
        private val view: PagerItemMediaViewerLivePhotoBinding,
        videoPlayerVHDelegate: VideoPlayerViewHolder = VideoPlayerViewHolderImpl(view.videoView),
    ) : FastAdapter.ViewHolder<LivePhotoViewerPage>(view.root),
        VideoPlayerViewHolder by videoPlayerVHDelegate,
        KoinScopeComponent {

        override val scope: Scope
            get() = getKoin().getScope(DI_SCOPE_SESSION)

        private val picasso: Picasso by inject()

        private val imageLoadingCallback = object : Callback {
            override fun onSuccess() =
                playIfContentIsReady()

            override fun onError(e: Exception?) {
                view.progressIndicator.hide()
                view.errorTextView.isVisible = true
            }
        }

        override fun attachToWindow(item: LivePhotoViewerPage) = with(playerView.player!!) {
            // The view state must be reset firstly on attach.
            view.progressIndicator.show()
            view.photoView.isVisible = false
            view.videoView.isVisible = false
            view.errorTextView.isVisible = false

            when (playbackState) {
                // When the player is stopped or buffering.
                Player.STATE_IDLE,
                Player.STATE_BUFFERING -> {
                    addListener(object : Player.Listener {
                        override fun onPlaybackStateChanged(playbackState: Int) {
                            if (playbackState == Player.STATE_READY) {
                                removeListener(this)
                                playIfContentIsReady()
                            }
                        }
                    })
                    if (playbackState == Player.STATE_IDLE) {
                        prepare()
                    }
                }

                // When the player is ready.
                Player.STATE_READY -> {
                    playIfContentIsReady()
                }

                // When the video is ended.
                // Occurs when the screen is re-created,
                // as the player is stopped beforehand in other cases.
                Player.STATE_ENDED -> {
                    // Immediately show the still image.
                    view.progressIndicator.hide()
                    view.photoView.isVisible = true
                }
            }
        }

        private fun playIfContentIsReady() {
            val player = playerView.player
                ?: return

            val isPhotoReady = view.photoView.drawable != null
            val isVideoReady = player.playbackState == Player.STATE_READY
            val isAttached = view.root.isAttachedToWindow

            if (isPhotoReady && isVideoReady && isAttached) {
                view.progressIndicator.hide()
                view.videoView.isVisible = true
                player.play()
                fadeIfCloseToTheEndOrTryLater()
            }
        }

        private fun fadeIfCloseToTheEndOrTryLater() {
            val player = playerView.player
            if (!playerView.isAttachedToWindow || player == null) {
                // Do not retry if the view is outdated.
                return
            }

            val currentPosition = player.currentPosition
            val duration = player.contentDuration

            if (duration > 0 && duration - currentPosition < FADE_DURATION_MS) {
                view.photoView.fadeIn(FADE_DURATION_MS)
            } else {
                // Retry with delay.
                view.videoView.postDelayed(::fadeIfCloseToTheEndOrTryLater, 50)
            }
        }

        // This method is called on swipe but not on screen destroy.
        // Screen lifecycle is handled in VideoPlayerViewHolder::bindPlayerToLifecycle.
        override fun detachFromWindow(item: LivePhotoViewerPage) = with(playerView.player!!) {
            // Stop playback once the page is swiped.
            stop()

            // Seek to default position to start playback from the beginning
            // when swiping back to this page.
            // This seems the only right place to call this method.
            seekToDefaultPosition()
        }

        override fun bindView(
            item: LivePhotoViewerPage,
            payloads: List<Any>,
        ) = with(playerCache.getPlayer(key = item.mediaId)) {
            playerView.player = this
            enableFatalPlaybackErrorListener(item)

            // Setting to false hides the controller automatically.
            view.videoView.useController = false

            // Only set up the player if its media item is changed.
            if (currentMediaItem?.mediaId != item.mediaId) {
                // Start playback close to the end, like in iOS gallery.
                val startPositionMs = ((item.fullVideoDurationMs ?: 0) - PLAYBACK_DURATION_MS)
                    .coerceAtLeast(0)

                setMediaItem(
                    MediaItem.Builder()
                        .setMediaId(item.mediaId)
                        .setClippingConfiguration(
                            MediaItem.ClippingConfiguration.Builder()
                                .setStartPositionMs(startPositionMs)
                                .build()
                        )
                        .setUri(item.videoPreviewUri)
                        // Assumption: PhotoPrism previews are always "video/mp4".
                        .setMimeType(MimeTypes.VIDEO_MP4)
                        .build()
                )

                volume = 0f
                repeatMode = Player.REPEAT_MODE_OFF

                // Start loading media on bind,
                // but prevent playback start until attached to the window
                // and the still image is ready.
                prepare()
                playWhenReady = false
            }

            view.photoView.setImageDrawable(null)
            picasso
                .load(item.photoPreviewUrl)
                .hardwareConfigIfAvailable()
                // Picasso deferred fit is no good when we we want to resize the image
                // considering the zoom factor, so the zoom actually makes sense.
                .resize(item.imageViewSize.width, item.imageViewSize.height)
                .centerInside()
                .onlyScaleDown()
                .into(view.photoView, imageLoadingCallback)
        }

        override fun unbindView(item: LivePhotoViewerPage) {
            picasso.cancelRequest(view.photoView)
            view.photoView.clearAnimation()
            playerView.player = null
            playerCache.releasePlayer(key = item.mediaId)
        }

        private companion object {
            const val FADE_DURATION_MS = 200
            const val PLAYBACK_DURATION_MS = 400 + FADE_DURATION_MS
        }
    }
}

