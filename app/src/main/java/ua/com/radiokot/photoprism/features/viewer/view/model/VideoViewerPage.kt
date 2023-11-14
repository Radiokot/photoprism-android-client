package ua.com.radiokot.photoprism.features.viewer.view.model

import android.net.Uri
import android.view.View
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import ua.com.radiokot.photoprism.R
import ua.com.radiokot.photoprism.databinding.PagerItemMediaViewerVideoBinding
import ua.com.radiokot.photoprism.features.gallery.data.model.GalleryMedia
import ua.com.radiokot.photoprism.features.viewer.view.MediaViewerPageViewHolder
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
    ) : MediaViewerPageViewHolder<VideoViewerPage>(view.root),
        VideoPlayerViewHolder by VideoPlayerViewHolderImpl(view.videoView) {

        override fun attachToWindow(item: VideoViewerPage) = with(view.videoView.player!!) {
            super.attachToWindow(item)

            if (isPlaying) {
                return@with
            }

            when (playbackState) {
                // When the player is stopped.
                Player.STATE_IDLE -> {
                    prepare()
                    playWhenReady = true
                }

                // When the player is loading.
                Player.STATE_BUFFERING -> {
                    playWhenReady = true
                }

                // When the player is ready.
                Player.STATE_READY -> {
                    play()
                }

                // When the video is ended.
                // Occurs when the screen is re-created,
                // as the player is stopped beforehand in other cases.
                Player.STATE_ENDED -> {
                    seekToDefaultPosition()
                    play()
                }
            }
        }

        // This method is called on swipe but not on screen destroy.
        // Screen lifecycle is handled in VideoPlayerViewHolder::bindPlayerToLifecycle.
        override fun detachFromWindow(item: VideoViewerPage) = with(view.videoView.player!!) {
            // Stop playback once the page is swiped.
            stop()

            // Seek to default position to start playback from the beginning
            // when swiping back to this page.
            // This seems the only right place to call this method.
            seekToDefaultPosition()
        }

        override fun bindView(
            item: VideoViewerPage,
            payloads: List<Any>,
        ) = with(playerCache.getPlayer(key = item.mediaId)) {
            super.bindView(item, payloads)

            view.videoView.player = this
            enableFatalPlaybackErrorListener(item)
            setOnPlaybackEndListener(::onContentPresented)

            if (item.needsVideoControls) {
                view.videoView.useController = true
                // If need to use the controller, show it manually.
                view.videoView.showController()
            } else {
                // Setting to false hides the controller automatically.
                view.videoView.useController = false
            }

            // Only set up the player if its media item is changed.
            if (currentMediaItem?.mediaId != item.mediaId) {
                setMediaItem(
                    MediaItem.Builder()
                        .setMediaId(item.mediaId)
                        .setUri(item.previewUri)
                        .build()
                )

                volume = 1f
                repeatMode =
                    if (item.isLooped)
                        Player.REPEAT_MODE_ONE
                    else
                        Player.REPEAT_MODE_OFF

                // Start loading media on bind,
                // but prevent playback start until attached to the window.
                prepare()
                playWhenReady = false
            }
        }

        override fun unbindView(item: VideoViewerPage) {
            view.videoView.player = null
            playerCache.releasePlayer(key = item.mediaId)
        }
    }
}
