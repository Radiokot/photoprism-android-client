package ua.com.radiokot.photoprism.features.viewer.view

import android.view.View
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.google.android.exoplayer2.PlaybackException
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.audio.AudioSink
import com.google.android.exoplayer2.decoder.DecoderException
import com.google.android.exoplayer2.ui.PlayerView
import ua.com.radiokot.photoprism.R
import ua.com.radiokot.photoprism.databinding.LayoutVideoPlayerControlsBinding
import ua.com.radiokot.photoprism.features.viewer.view.model.MediaViewerPage

class VideoPlayerViewHolderImpl(
    val videoView: PlayerView,
) : VideoPlayerViewHolder {
    override val player: Player?
        get() = videoView.player

    var fatalPlaybackErrorListener: (MediaViewerPage) -> Unit = {}

    private val lifecycleObserver = object : DefaultLifecycleObserver {
        private var playerHasBeenPaused = false

        override fun onPause(owner: LifecycleOwner) {
            val player = videoView.player
                ?: return

            videoView.post {
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
            val player = videoView.player
                ?: return

            if (player.playbackState != Player.STATE_IDLE && playerHasBeenPaused) {
                player.playWhenReady = true
            }
        }
    }

    override val playerControlsLayout: LayoutVideoPlayerControlsBinding? by lazy {
        videoView.findViewById<View>(R.id.player_controls_layout)
            ?.let(LayoutVideoPlayerControlsBinding::bind)
    }

    private var _playerCache: VideoPlayerCache? = null
    override var playerCache: VideoPlayerCache
        get() = checkNotNull(_playerCache) { "Player cache must be set" }
        set(value) {
            _playerCache = value
        }

    override fun bindPlayerToLifecycle(lifecycle: Lifecycle) {
        lifecycle.addObserver(lifecycleObserver)
    }

    override fun onAttachToWindow(
        mediaId: String,
        item: MediaViewerPage
    ): Player {
        val player = playerCache.getPlayer(key = mediaId)
        videoView.player = player

        val theOnlyFatalExceptionListener = TheOnlyPlayerFatalPlaybackExceptionListener {
            fatalPlaybackErrorListener(item)
        }
        player.removeListener(theOnlyFatalExceptionListener)
        player.addListener(theOnlyFatalExceptionListener)

        return player
    }

    override fun onDetachFromWindow(item: MediaViewerPage) {
        player?.apply {
            stop()
            seekToDefaultPosition()
        }
    }

    override fun setOnFatalPlaybackErrorListener(listener: (source: MediaViewerPage) -> Unit) {
        fatalPlaybackErrorListener = listener
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
