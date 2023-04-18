package ua.com.radiokot.photoprism.features.viewer.view.model

import android.net.Uri
import android.view.View
import com.google.android.exoplayer2.Player
import com.mikepenz.fastadapter.FastAdapter
import ua.com.radiokot.photoprism.R
import ua.com.radiokot.photoprism.databinding.PagerItemMediaViewerVideoBinding
import ua.com.radiokot.photoprism.extension.checkNotNull
import ua.com.radiokot.photoprism.features.viewer.view.VideoPlayerCache

class VideoViewerPage(
    previewUrl: String,
    val isLooped: Boolean,
    val needsVideoControls: Boolean,
    thumbnailUrl: String,
) : MediaViewerPage(thumbnailUrl) {
    val previewUri: Uri = Uri.parse(previewUrl)

    override val type: Int
        get() = R.id.pager_item_media_viewer_video

    override val layoutRes: Int
        get() = R.layout.pager_item_media_viewer_video

    override fun getViewHolder(v: View): ViewHolder =
        ViewHolder(v)

    class ViewHolder(itemView: View) : FastAdapter.ViewHolder<VideoViewerPage>(itemView) {
        val view = PagerItemMediaViewerVideoBinding.bind(itemView)
        var playerCache: VideoPlayerCache? = null

        override fun attachToWindow(item: VideoViewerPage) {
            val playerCache = this.playerCache.checkNotNull {
                "Player cache must be set"
            }

            val player = playerCache.getPlayer(
                mediaSourceUri = item.previewUri,
                context = view.videoView.context,
            )

            view.videoView.useController = item.needsVideoControls

            player.apply {
                repeatMode =
                    if (item.isLooped) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF

                if (!player.isPlaying) {
                    prepare()
                    play()
                }
            }

            view.videoView.player = player
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
    }
}