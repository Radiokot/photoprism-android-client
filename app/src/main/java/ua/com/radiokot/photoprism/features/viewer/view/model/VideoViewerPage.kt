package ua.com.radiokot.photoprism.features.viewer.view.model

import android.net.Uri
import android.view.View
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.PlaybackException
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.audio.AudioSink
import com.google.android.exoplayer2.decoder.DecoderException
import com.google.android.exoplayer2.util.MimeTypes
import com.mikepenz.fastadapter.FastAdapter
import ua.com.radiokot.photoprism.R
import ua.com.radiokot.photoprism.databinding.PagerItemMediaViewerVideoBinding
import ua.com.radiokot.photoprism.extension.checkNotNull
import ua.com.radiokot.photoprism.features.gallery.data.model.GalleryMedia
import ua.com.radiokot.photoprism.features.viewer.view.VideoPlayer
import ua.com.radiokot.photoprism.features.viewer.view.VideoPlayerCache

class VideoViewerPage(
    previewUrl: String,
    val isLooped: Boolean,
    val needsVideoControls: Boolean,
    thumbnailUrl: String,
    source: GalleryMedia?,
) : MediaViewerPage(thumbnailUrl, source) {
    val previewUri: Uri = Uri.parse(previewUrl)
    val mediaId: String = previewUrl.hashCode().toString()

    override val type: Int
        get() = R.id.pager_item_media_viewer_video

    override val layoutRes: Int
        get() = R.layout.pager_item_media_viewer_video

    override fun getViewHolder(v: View): ViewHolder =
        ViewHolder(v)

    class ViewHolder(itemView: View) : FastAdapter.ViewHolder<VideoViewerPage>(itemView) {
        val view = PagerItemMediaViewerVideoBinding.bind(itemView)
        var playerCache: VideoPlayerCache? = null
        var fatalPlaybackErrorListener: (VideoViewerPage) -> Unit = {}

        fun bindToLifecycle(lifecycle: Lifecycle) {
            lifecycle.addObserver(object : DefaultLifecycleObserver {
                private var playerHasBeenPaused = false

                override fun onPause(owner: LifecycleOwner) {
                    val player = view.videoView.player
                        ?: return

                    view.videoView.post {
                        // Only pause if the owner is not destroyed.
                        if (owner.lifecycle.currentState.isAtLeast(Lifecycle.State.CREATED)
                            && player.isPlaying
                        ) {
                            player.pause()
                            playerHasBeenPaused = true
                        }
                    }
                }

                override fun onResume(owner: LifecycleOwner) {
                    val player = view.videoView.player
                        ?: return

                    if (player.playbackState != Player.STATE_IDLE && playerHasBeenPaused) {
                        player.playWhenReady = true
                    }
                }
            })
        }

        override fun attachToWindow(item: VideoViewerPage) {
            view.videoView.useController = item.needsVideoControls

            val playerCache = this.playerCache.checkNotNull {
                "Player cache must be set"
            }

            val player = playerCache.getPlayer(key = item.mediaId)
            setUpPlayer(player, item)
            view.videoView.player = player
        }

        private fun setUpPlayer(player: VideoPlayer, item: VideoViewerPage) = with(player) {
            if (currentMediaItem?.mediaId != item.mediaId) {
                setMediaItem(
                    MediaItem.Builder()
                        .setMediaId(item.mediaId)
                        .setUri(item.previewUri)
                        .setMimeType(MimeTypes.VIDEO_MP4)
                        .build()
                )

                repeatMode =
                    if (item.isLooped)
                        Player.REPEAT_MODE_ONE
                    else
                        Player.REPEAT_MODE_OFF
            }

            // Only play automatically on init.
            if (!isPlaying && playbackState == Player.STATE_IDLE) {
                playWhenReady = true
            }
            prepare()

            val theOnlyFatalExceptionListener = TheOnlyPlayerFatalPlaybackExceptionListener {
                fatalPlaybackErrorListener(item)
            }
            player.removeListener(theOnlyFatalExceptionListener)
            player.addListener(theOnlyFatalExceptionListener)
        }

        override fun detachFromWindow(item: VideoViewerPage) {
            view.videoView.player?.apply {
                stop()
                seekToDefaultPosition()
            }
        }

        // Video player must be set up only once it is attached.
        override fun bindView(item: VideoViewerPage, payloads: List<Any>) {
        }

        override fun unbindView(item: VideoViewerPage) {
        }

        /**
         * An error listener that triggers when a fatal playback exception occurs.
         * The player can only have one of such listener as all the instances are equal.
         */
        private class TheOnlyPlayerFatalPlaybackExceptionListener(
            private val onError: (Throwable) -> Unit,
        ) : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                when (val cause = error.cause) {
                    is DecoderException,
                    is AudioSink.InitializationException ->
                        onError(cause)
                }
            }

            override fun equals(other: Any?): Boolean {
                return other is TheOnlyPlayerFatalPlaybackExceptionListener
            }

            override fun hashCode(): Int {
                return 333
            }
        }
    }
}