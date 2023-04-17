package ua.com.radiokot.photoprism.features.viewer.view.model

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.view.View
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.items.AbstractItem
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import org.koin.core.component.KoinScopeComponent
import org.koin.core.component.inject
import org.koin.core.scope.Scope
import ua.com.radiokot.photoprism.R
import ua.com.radiokot.photoprism.databinding.PagerItemMediaViewerImageBinding
import ua.com.radiokot.photoprism.databinding.PagerItemMediaViewerUnsupportedBinding
import ua.com.radiokot.photoprism.databinding.PagerItemMediaViewerVideoBinding
import ua.com.radiokot.photoprism.di.DI_SCOPE_SESSION
import ua.com.radiokot.photoprism.features.gallery.data.model.GalleryMedia
import ua.com.radiokot.photoprism.features.gallery.view.model.GalleryMediaTypeResources

sealed class MediaViewerPagerItem(
    val thumbnailUrl: String,
) : AbstractItem<ViewHolder>() {

    override var identifier: Long
        get() = thumbnailUrl.hashCode().toLong()
        set(_) = error("Don't override my value")

    class ImageViewer(
        val previewUrl: String,
        thumbnailUrl: String,
    ) : MediaViewerPagerItem(thumbnailUrl) {
        override val type: Int
            get() = R.id.pager_item_media_viewer_image

        override val layoutRes: Int
            get() = R.layout.pager_item_media_viewer_image

        override fun getViewHolder(v: View): ViewHolder =
            ViewHolder(v)

        class ViewHolder(
            itemView: View,
        ) : FastAdapter.ViewHolder<ImageViewer>(itemView), KoinScopeComponent {
            override val scope: Scope
                get() = getKoin().getScope(DI_SCOPE_SESSION)

            val view = PagerItemMediaViewerImageBinding.bind(itemView)
            private val picasso: Picasso by inject()

            private val imageLoadingCallback = object : Callback {
                override fun onSuccess() {
                    view.progressIndicator.hide()
                }

                override fun onError(e: Exception?) {
                    view.progressIndicator.hide()
                    view.errorTextView.visibility = View.VISIBLE
                }
            }

            override fun bindView(item: ImageViewer, payloads: List<Any>) {
                view.progressIndicator.show()
                view.errorTextView.visibility = View.GONE

                picasso
                    .load(item.previewUrl)
                    .into(view.photoView, imageLoadingCallback)
            }

            override fun unbindView(item: ImageViewer) {
                picasso.cancelRequest(view.photoView)
            }
        }
    }

    class VideoViewer(
        val previewUrl: String,
        val isLooped: Boolean,
        thumbnailUrl: String,
    ) : MediaViewerPagerItem(thumbnailUrl) {
        override val type: Int
            get() = R.id.pager_item_media_viewer_video

        override val layoutRes: Int
            get() = R.layout.pager_item_media_viewer_video

        override fun getViewHolder(v: View): ViewHolder =
            ViewHolder(v)

        class ViewHolder(itemView: View) : FastAdapter.ViewHolder<VideoViewer>(itemView) {
            val view = PagerItemMediaViewerVideoBinding.bind(itemView)

            init {
                with(view.playPauseButton) {
                    setOnClickListener {
                        if (view.videoView.isPlaying) {
                            view.videoView.pause()
                        } else {
                            view.videoView.start()
                        }
                        updatePlayPause()
                    }
                }
            }

            private fun updatePlayPause() {
                view.playPauseButton.setIconResource(
                    if (view.videoView.isPlaying)
                        R.drawable.ic_pause
                    else
                        R.drawable.ic_play
                )
            }

            override fun bindView(item: VideoViewer, payloads: List<Any>) {
                view.playPauseButton.isVisible = false
            }

            // Video preparation must be done here,
            // as the binding takes place when the view is not yet attached.
            override fun attachToWindow(item: VideoViewer) {
                view.videoView.setDataSource(view.videoView.context, Uri.parse(item.previewUrl))
                view.videoView.prepareAsyncWhenSurfaceAvailable { mediaPlayer ->
                    mediaPlayer.isLooping = item.isLooped
                    mediaPlayer.setScreenOnWhilePlaying(true)
                    mediaPlayer.setOnCompletionListener {
                        updatePlayPause()
                    }
                    mediaPlayer.start()
                    updatePlayPause()
                    view.playPauseButton.isVisible = true
                }
            }

            override fun unbindView(item: VideoViewer) {
                view.videoView.stop()
            }
        }
    }

    class Unsupported(
        @StringRes
        val mediaTypeName: Int,
        @DrawableRes
        val mediaTypeIcon: Int?,
        thumbnailUrl: String,
    ) : MediaViewerPagerItem(thumbnailUrl) {
        override val type: Int
            get() = R.id.pager_item_media_viewer_unsupported

        override val layoutRes: Int
            get() = R.layout.pager_item_media_viewer_unsupported

        override fun getViewHolder(v: View): ViewHolder =
            ViewHolder(v)

        class ViewHolder(
            itemView: View,
        ) : FastAdapter.ViewHolder<Unsupported>(itemView), KoinScopeComponent {
            override val scope: Scope
                get() = getKoin().getScope(DI_SCOPE_SESSION)

            private val view = PagerItemMediaViewerUnsupportedBinding.bind(itemView)
            private val picasso: Picasso by inject()

            private val imageLoadingCallback = object : Callback {
                override fun onSuccess() {
                    view.progressIndicator.hide()
                }

                override fun onError(e: Exception?) {
                    view.progressIndicator.hide()
                    view.errorTextView.visibility = View.VISIBLE
                }
            }

            override fun bindView(item: Unsupported, payloads: List<Any>) {
                view.progressIndicator.show()
                view.errorTextView.visibility = View.GONE

                with(view.mediaTypeTextView) {
                    setText(item.mediaTypeName)
                    setCompoundDrawablesRelativeWithIntrinsicBounds(
                        item.mediaTypeIcon?.let { ContextCompat.getDrawable(context, it) },
                        null,
                        null,
                        null
                    )
                }

                picasso
                    .load(item.thumbnailUrl)
                    .placeholder(ColorDrawable(Color.LTGRAY))
                    .fit()
                    .into(view.thumbnailImageView, imageLoadingCallback)
            }

            override fun unbindView(item: Unsupported) {
                picasso.cancelRequest(view.thumbnailImageView)
            }
        }
    }

    companion object {
        fun fromGalleryMedia(source: GalleryMedia): MediaViewerPagerItem {
            return when (source.media) {
                is GalleryMedia.TypeData.ViewableAsImage ->
                    ImageViewer(
                        previewUrl = source.media.hdPreviewUrl,
                        thumbnailUrl = source.smallThumbnailUrl,
                    )
                is GalleryMedia.TypeData.ViewableAsVideo ->
                    VideoViewer(
                        previewUrl = source.media.avcPreviewUrl,
                        isLooped = source.media.isLooped,
                        thumbnailUrl = source.smallThumbnailUrl,
                    )
                else ->
                    Unsupported(
                        mediaTypeIcon = GalleryMediaTypeResources.getIcon(source.media.typeName),
                        mediaTypeName = GalleryMediaTypeResources.getName(source.media.typeName),
                        thumbnailUrl = source.smallThumbnailUrl,
                    )
            }
        }
    }
}