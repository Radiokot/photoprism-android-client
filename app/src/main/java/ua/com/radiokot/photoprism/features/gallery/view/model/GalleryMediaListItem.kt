package ua.com.radiokot.photoprism.features.gallery.view.model

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.View
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.items.AbstractItem
import com.squareup.picasso.Picasso
import ua.com.radiokot.photoprism.R
import ua.com.radiokot.photoprism.databinding.ListItemGalleryMediaBinding
import ua.com.radiokot.photoprism.features.gallery.data.model.GalleryMedia

class GalleryMediaListItem(
    val thumbnailUrl: String,
    val name: String,
    @DrawableRes
    val mediaTypeIcon: Int?,
    @StringRes
    val mediaTypeName: Int?,
    val source: GalleryMedia?
) : AbstractItem<GalleryMediaListItem.ViewHolder>() {

    constructor(source: GalleryMedia) : this(
        thumbnailUrl = source.smallThumbnailUrl,
        name = source.name,
        mediaTypeIcon = when (source.media) {
            GalleryMedia.MediaType.Image ->
                null
            GalleryMedia.MediaType.Animated ->
                R.drawable.ic_animation
            GalleryMedia.MediaType.Live ->
                R.drawable.ic_live_photo
            GalleryMedia.MediaType.Other ->
                R.drawable.ic_sledding
            GalleryMedia.MediaType.Raw ->
                R.drawable.ic_raw
            GalleryMedia.MediaType.Sidecar ->
                R.drawable.ic_attachment
            GalleryMedia.MediaType.Text ->
                R.drawable.ic_text
            GalleryMedia.MediaType.Unknown ->
                R.drawable.ic_unknown
            GalleryMedia.MediaType.Vector ->
                R.drawable.ic_curve
            GalleryMedia.MediaType.Video ->
                R.drawable.ic_video
        },
        mediaTypeName = when (source.media) {
            GalleryMedia.MediaType.Image ->
                null
            GalleryMedia.MediaType.Animated ->
                R.string.media_type_animated
            GalleryMedia.MediaType.Live ->
                R.string.media_type_live
            GalleryMedia.MediaType.Other ->
                R.string.media_type_other
            GalleryMedia.MediaType.Raw ->
                R.string.media_type_raw
            GalleryMedia.MediaType.Sidecar ->
                R.string.media_type_sidecar
            GalleryMedia.MediaType.Text ->
                R.string.media_type_text
            GalleryMedia.MediaType.Unknown ->
                R.string.media_type_unknown
            GalleryMedia.MediaType.Vector ->
                R.string.media_type_vector
            GalleryMedia.MediaType.Video ->
                R.string.media_type_video
        },
        source = source,
    )

    override var identifier: Long
        get() = source?.hashCode()?.toLong() ?: hashCode().toLong()
        set(_) {
            throw IllegalStateException("Do not overwrite my value!")
        }

    override val type: Int
        get() = R.id.list_item_gallery_media

    override val layoutRes: Int
        get() = R.layout.list_item_gallery_media

    override fun getViewHolder(v: View): ViewHolder =
        ViewHolder(v)

    class ViewHolder(itemView: View) : FastAdapter.ViewHolder<GalleryMediaListItem>(itemView) {
        private val view = ListItemGalleryMediaBinding.bind(itemView)

        override fun bindView(item: GalleryMediaListItem, payloads: List<Any>) {
            view.imageView.contentDescription = item.name

            Picasso.get()
                .load(item.thumbnailUrl)
                .placeholder(ColorDrawable(Color.LTGRAY))
                .fit()
                .centerCrop()
                .into(view.imageView)

            with(view.mediaTypeImageView) {
                if (item.mediaTypeIcon != null) {
                    visibility = View.VISIBLE
                    setImageResource(item.mediaTypeIcon)
                } else {
                    visibility = View.GONE
                }

                if (item.mediaTypeName != null) {
                    contentDescription = context.getString(item.mediaTypeName)
                } else {
                    contentDescription = null
                }
            }
        }

        override fun unbindView(item: GalleryMediaListItem) {
            Picasso.get().cancelRequest(view.imageView)
        }
    }
}