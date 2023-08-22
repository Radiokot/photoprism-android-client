package ua.com.radiokot.photoprism.features.viewer.view.model

import android.net.Uri
import android.util.Size
import android.view.View
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.util.MimeTypes
import com.mikepenz.fastadapter.FastAdapter
import com.squareup.picasso.Callback.EmptyCallback
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

        private val imageLoadingCallback = object : EmptyCallback() {
            override fun onError(e: Exception?) {
                view.errorTextView.visibility = View.VISIBLE
            }
        }
        private val playerListener = TheOnlyPlayerPlayingChangeListener {
            view.videoView.postDelayed(::fadeIfCloseToTheEnd, 50)
        }

        override fun attachToWindow(item: LivePhotoViewerPage) {
            view.videoView.useController = false

            onAttachToWindow(
                mediaId = item.mediaId,
                item = item,
            )
            player?.let { setUpPlayer(it, item) }
        }

        private fun setUpPlayer(player: Player, item: LivePhotoViewerPage) = with(player) {
            if (currentMediaItem?.mediaId != item.mediaId) {
                setMediaItem(
                    MediaItem.Builder()
                        .setMediaId(item.mediaId)
                        .setUri(item.videoPreviewUri)
                        // Assumption: PhotoPrism previews are always "video/mp4".
                        .setMimeType(MimeTypes.VIDEO_MP4)
                        .build(),
                    // TODO: Set start time close to the end.
                )

                repeatMode = Player.REPEAT_MODE_OFF
            }

            // Only play automatically on init.
            if (!isPlaying && playbackState == Player.STATE_IDLE) {
                playWhenReady = true
            }
            prepare()

            // Make the still image fade in when the video is close to the end.
            // The current position needs to be continuously polled when playing,
            // as there is no live listener.
            player.addListener(playerListener)
        }

        private fun fadeIfCloseToTheEnd() {
            val player = player
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

        override fun detachFromWindow(item: LivePhotoViewerPage) {
            onDetachFromWindow(item)
            player?.removeListener(playerListener)
        }

        override fun bindView(item: LivePhotoViewerPage, payloads: List<Any>) {
            view.errorTextView.visibility = View.GONE
            // Photo to be shown when the video part is ended.
            view.photoView.visibility = View.GONE

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
        }
    }

    private class TheOnlyPlayerPlayingChangeListener(
        private val listener: (isPlaying: Boolean) -> Unit,
    ) : Player.Listener {

        override fun onIsPlayingChanged(isPlaying: Boolean) =
            listener(isPlaying)

        override fun equals(other: Any?): Boolean {
            return other is TheOnlyPlayerPlayingChangeListener
        }

        override fun hashCode(): Int {
            return 444
        }
    }

    private companion object {
        const val FADE_DURATION_MS = 200
    }
}

