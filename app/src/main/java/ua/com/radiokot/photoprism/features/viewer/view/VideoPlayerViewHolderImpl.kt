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
    override val playerView: PlayerView,
) : VideoPlayerViewHolder {

    private var fatalPlaybackErrorListener: (MediaViewerPage) -> Unit = {}

    /**
     * An observer which pauses playback when the screen is paused (but not destroyed)
     * and resumes it when the screen is resumed.
     *
     * This has nothing to do with the screen rotation.
     */
    private val pauseLifecycleObserver = object : DefaultLifecycleObserver {
        private var playerHasBeenPaused = false

        override fun onPause(owner: LifecycleOwner) {
            playerHasBeenPaused = false

            val player = playerView.player
                ?: return // It's ok if there is no player at this moment for some reason.

            playerView.post {
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
            val player = playerView.player
                ?: return // It's ok if there is no player at this moment, it may be creation.

            if (player.playbackState != Player.STATE_IDLE && playerHasBeenPaused) {
                player.playWhenReady = true
            }
        }
    }

    override val playerControlsLayout: LayoutVideoPlayerControlsBinding? by lazy {
        playerView.findViewById<View>(R.id.player_controls_layout)
            ?.let(LayoutVideoPlayerControlsBinding::bind)
    }

    private var _playerCache: VideoPlayerCache? = null
    override var playerCache: VideoPlayerCache
        get() = checkNotNull(_playerCache) { "Player cache must be set" }
        set(value) {
            _playerCache = value
        }

    override fun bindPlayerToLifecycle(lifecycle: Lifecycle) {
        lifecycle.addObserver(pauseLifecycleObserver)
    }

    override fun setOnFatalPlaybackErrorListener(listener: (source: MediaViewerPage) -> Unit) {
        fatalPlaybackErrorListener = listener
    }

    override fun enableFatalPlaybackErrorListener(item: MediaViewerPage) {
        playerView.player?.apply {
            val theOnlyFatalExceptionListener = TheOnlyPlayerFatalPlaybackExceptionListener {
                fatalPlaybackErrorListener(item)
            }
            removeListener(theOnlyFatalExceptionListener)
            addListener(theOnlyFatalExceptionListener)
        }
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
