package ua.com.radiokot.photoprism.features.gallery.view.model

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.View
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.items.AbstractItem
import com.squareup.picasso.Picasso
import ua.com.radiokot.photoprism.R
import ua.com.radiokot.photoprism.databinding.ListItemGalleryMediaBinding
import ua.com.radiokot.photoprism.features.gallery.data.model.GalleryMedia

class GalleryMediaListItem(
    val thumbnailUrl: String,
    val name: String,
    val source: GalleryMedia?
) : AbstractItem<GalleryMediaListItem.ViewHolder>() {

    constructor(source: GalleryMedia) : this(
        thumbnailUrl = source.smallThumbnailUrl,
        name = source.name,
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
        }

        override fun unbindView(item: GalleryMediaListItem) {
            Picasso.get().cancelRequest(view.imageView)
        }
    }
}