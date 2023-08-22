package ua.com.radiokot.photoprism.features.viewer.view

import androidx.lifecycle.Lifecycle
import com.google.android.exoplayer2.Player
import ua.com.radiokot.photoprism.databinding.LayoutVideoPlayerControlsBinding
import ua.com.radiokot.photoprism.features.viewer.view.model.MediaViewerPage

interface VideoPlayerViewHolder {
    val player: Player?
    val playerControlsLayout: LayoutVideoPlayerControlsBinding?
    var playerCache: VideoPlayerCache

    fun onAttachToWindow(
        mediaId: String,
        item: MediaViewerPage,
    )

    fun onDetachFromWindow(item: MediaViewerPage)
    fun bindPlayerToLifecycle(lifecycle: Lifecycle)
    fun setOnFatalPlaybackErrorListener(listener: (item: MediaViewerPage) -> Unit)
}
