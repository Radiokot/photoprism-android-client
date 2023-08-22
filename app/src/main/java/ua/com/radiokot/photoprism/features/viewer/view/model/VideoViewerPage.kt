package ua.com.radiokot.photoprism.features.viewer.view.model

import android.net.Uri
import android.view.View
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.util.MimeTypes
import com.mikepenz.fastadapter.FastAdapter
import ua.com.radiokot.photoprism.R
import ua.com.radiokot.photoprism.databinding.PagerItemMediaViewerVideoBinding
import ua.com.radiokot.photoprism.features.gallery.data.model.GalleryMedia
import ua.com.radiokot.photoprism.features.viewer.view.VideoPlayerViewHolder
import ua.com.radiokot.photoprism.features.viewer.view.VideoPlayerViewHolderImpl

class VideoViewerPage(
    previewUrl: String,
    val isLooped: Boolean,
    val needsVideoControls: Boolean,
    thumbnailUrl: String,
    source: GalleryMedia?,
) : MediaViewerPage(thumbnailUrl, source) {
    val previewUri: Uri = Uri.parse(previewUrl)
    val mediaId: String = identifier.toString()

    override val type: Int
        get() = R.id.pager_item_media_viewer_video

    override val layoutRes: Int
        get() = R.layout.pager_item_media_viewer_video

    override fun getViewHolder(v: View): ViewHolder =
        ViewHolder(PagerItemMediaViewerVideoBinding.bind(v))

    class ViewHolder(
        val view: PagerItemMediaViewerVideoBinding,
        delegate: VideoPlayerViewHolder = VideoPlayerViewHolderImpl(view.videoView),
    ) : FastAdapter.ViewHolder<VideoViewerPage>(view.root),
        VideoPlayerViewHolder by delegate {

        override fun attachToWindow(item: VideoViewerPage) {
            if (item.needsVideoControls) {
                view.videoView.useController = true
                // If need to use the controller, show it manually.
                view.videoView.showController()
            } else {
                // Setting to false hides the controller automatically.
                view.videoView.useController = false
            }

            onAttachToWindow(
                mediaId = item.mediaId,
                item = item,
            )
            player?.let { setUpPlayer(it, item) }
        }

        private fun setUpPlayer(player: Player, item: VideoViewerPage) = with(player) {
            if (currentMediaItem?.mediaId != item.mediaId) {
                setMediaItem(
                    MediaItem.Builder()
                        .setMediaId(item.mediaId)
                        .setUri(item.previewUri)
                        // Assumption: PhotoPrism previews are always "video/mp4".
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
        }

        override fun detachFromWindow(item: VideoViewerPage) =
            onDetachFromWindow(item)

        // Video player must be set up only once it is attached.
        override fun bindView(item: VideoViewerPage, payloads: List<Any>) {
        }

        override fun unbindView(item: VideoViewerPage) {
        }
    }
}
