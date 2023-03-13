package ua.com.radiokot.photoprism.features.viewer.view.model

import android.view.View
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.items.AbstractItem
import com.squareup.picasso.Picasso
import ua.com.radiokot.photoprism.R
import ua.com.radiokot.photoprism.databinding.PagerItemMediaViewerImageBinding
import ua.com.radiokot.photoprism.databinding.PagerItemMediaViewerUnsupportedBinding
import ua.com.radiokot.photoprism.features.gallery.data.model.GalleryMedia
import ua.com.radiokot.photoprism.features.gallery.view.model.GalleryMediaTypeResources

sealed class MediaViewerPageItem(
    val thumbnailUrl: String,
) : AbstractItem<ViewHolder>() {

    class Image(
        val previewUrl: String,
        thumbnailUrl: String,
    ) : MediaViewerPageItem(thumbnailUrl) {
        override val type: Int
            get() = R.id.pager_item_media_viewer_image

        override val layoutRes: Int
            get() = R.layout.pager_item_media_viewer_image

        override fun getViewHolder(v: View): ViewHolder =
            ViewHolder(v)

        class ViewHolder(itemView: View) : FastAdapter.ViewHolder<Image>(itemView) {
            private val view = PagerItemMediaViewerImageBinding.bind(itemView)

            override fun bindView(item: Image, payloads: List<Any>) {
                Picasso.get()
                    .load(item.previewUrl)
                    .into(view.photoView)
            }

            override fun unbindView(item: Image) {
                Picasso.get().cancelRequest(view.photoView)
            }
        }
    }

    class Unsupported(
        @StringRes
        val mediaTypeName: Int,
        @DrawableRes
        val mediaTypeIcon: Int?,
        thumbnailUrl: String,
    ) : MediaViewerPageItem(thumbnailUrl) {
        override val type: Int
            get() = R.id.pager_item_media_viewer_unsupported

        override val layoutRes: Int
            get() = R.layout.pager_item_media_viewer_unsupported

        override fun getViewHolder(v: View): ViewHolder =
            ViewHolder(v)

        class ViewHolder(itemView: View) : FastAdapter.ViewHolder<Unsupported>(itemView) {
            private val view = PagerItemMediaViewerUnsupportedBinding.bind(itemView)

            override fun bindView(item: Unsupported, payloads: List<Any>) {
                with(view.mediaTypeTextView) {
                    setText(item.mediaTypeName)
                    setCompoundDrawablesRelativeWithIntrinsicBounds(
                        item.mediaTypeIcon?.let { ContextCompat.getDrawable(context, it) },
                        null,
                        null,
                        null
                    )
                }

                Picasso.get()
                    .load(item.thumbnailUrl)
                    .fit()
                    .into(view.thumbnailImageView)
            }

            override fun unbindView(item: Unsupported) {
                Picasso.get().cancelRequest(view.thumbnailImageView)
            }
        }
    }

    companion object {
        fun fromGalleryMedia(source: GalleryMedia): MediaViewerPageItem {
            return when (source.media) {
                is GalleryMedia.TypeData.Image ->
                    Image(
                        previewUrl = source.media.hdPreviewUrl,
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