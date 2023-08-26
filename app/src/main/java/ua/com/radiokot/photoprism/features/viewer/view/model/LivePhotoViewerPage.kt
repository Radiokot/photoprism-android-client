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
        delegate: VideoPlayerViewHolder = VideoPlayerViewHolderImpl(view.videoView),
    ) : FastAdapter.ViewHolder<LivePhotoViewerPage>(view.root),
        VideoPlayerViewHolder by delegate,
        KoinScopeComponent {

        override val scope: Scope
            get() = getKoin().getScope(DI_SCOPE_SESSION)

        private val picasso: Picasso by inject()

        private val imageLoadingCallback = object : Callback {
            override fun onSuccess() {
                isPhotoReady = true
                playIfContentIsReady()
            }

            override fun onError(e: Exception?) {
                view.progressIndicator.hide()
                view.errorTextView.isVisible = true
            }
        }
        private val playerListener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (isPlaying) {
                    view.videoView.postDelayed(this@ViewHolder::fadeIfCloseToTheEnd, 50)
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    isVideoReady = true
                    playIfContentIsReady()
                }
            }
        }
        private var isVideoReady = false
        private var isPhotoReady = false

        override fun attachToWindow(item: LivePhotoViewerPage) {
            view.videoView.useController = false
            view.errorTextView.isVisible = false

            playerCache.getPlayer(key = item.mediaId).apply {
                playerView.player = this
                enableFatalPlaybackErrorListener(item)

                if (currentMediaItem?.mediaId != item.mediaId) {
                    // Start playback close to the end, like in iOS gallery.
                    val startPositionMs = ((item.fullVideoDurationMs ?: 0) - PLAYBACK_DURATION_MS)
                        .coerceAtLeast(0)

                    setMediaItem(
                        MediaItem.Builder()
                            .setMediaId(item.mediaId)
                            .setUri(item.videoPreviewUri)
                            .setClippingConfiguration(
                                MediaItem.ClippingConfiguration.Builder()
                                    .setStartPositionMs(startPositionMs)
                                    .build()
                            )
                            // Assumption: PhotoPrism previews are always "video/mp4".
                            .setMimeType(MimeTypes.VIDEO_MP4)
                            .build()
                    )

                    repeatMode = Player.REPEAT_MODE_OFF
                    volume = 0f
                }

                playWhenReady = false
                removeListener(playerListener)
                addListener(playerListener)

                when (val playbackState = playbackState) {
                    // Idle
                    Player.STATE_IDLE,
                    Player.STATE_BUFFERING -> {
                        isVideoReady = false

                        view.videoView.isVisible = false
                        view.photoView.isVisible = false
                        view.progressIndicator.show()

                        if (playbackState == Player.STATE_IDLE) {
                            prepare()
                        }
                    }

                    Player.STATE_READY -> {
                        isVideoReady = true

                        view.videoView.isVisible = true
                        view.photoView.isVisible = false
                        view.progressIndicator.hide()

                        playIfContentIsReady()
                    }

                    Player.STATE_ENDED -> {
                        isVideoReady = true

                        view.videoView.isVisible = false
                        view.photoView.isVisible = true
                        view.progressIndicator.hide()
                    }
                }
            }
        }

        private fun playIfContentIsReady() {
            if (isPhotoReady && isVideoReady) {
                view.videoView.isVisible = true
                view.progressIndicator.hide()
                playerView.player?.play()
            }
        }

        private fun fadeIfCloseToTheEnd() {
            val player = playerView.player
                ?: return

            if (player.isPlaying) {
                val currentPosition = player.currentPosition
                val duration = player.contentDuration

                if (duration > 0 && duration - currentPosition < FADE_DURATION_MS) {
                    view.photoView.fadeIn(FADE_DURATION_MS)
                } else {
                    view.videoView.postDelayed(::fadeIfCloseToTheEnd, 50)
                }
            }
        }

        // This method is called on swipe but not on screen destroy.
        // Screen lifecycle is handled in VideoPlayerViewHolder::bindPlayerToLifecycle.
        override fun detachFromWindow(item: LivePhotoViewerPage) = with(playerView.player!!) {
            removeListener(playerListener)

            // Stop playback once the page is swiped.
            stop()

            // Seek to default position to start playback from the beginning
            // when swiping back to this page.
            // This seems the only right place to call this method.
            seekToDefaultPosition()
        }

        override fun bindView(item: LivePhotoViewerPage, payloads: List<Any>) {
            isPhotoReady = false

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

