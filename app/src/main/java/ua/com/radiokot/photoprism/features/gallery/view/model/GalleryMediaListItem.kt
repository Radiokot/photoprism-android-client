package ua.com.radiokot.photoprism.features.gallery.view.model

import android.graphics.Color
import android.view.View
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.items.AbstractItem
import ua.com.radiokot.photoprism.R
import ua.com.radiokot.photoprism.databinding.ListItemGalleryMediaBinding

class GalleryMediaListItem(
    // TODO: Fill it.
) : AbstractItem<GalleryMediaListItem.ViewHolder>() {

    override val type: Int
        get() = R.id.list_item_gallery_media
    override val layoutRes: Int
        get() = R.layout.list_item_gallery_media

    override fun getViewHolder(v: View): ViewHolder =
        ViewHolder(v)

    class ViewHolder(itemView: View) : FastAdapter.ViewHolder<GalleryMediaListItem>(itemView) {
        private val binding = ListItemGalleryMediaBinding.bind(itemView)

        override fun bindView(item: GalleryMediaListItem, payloads: List<Any>) {
            binding.imageView.setBackgroundColor(Color.RED)
        }

        override fun unbindView(item: GalleryMediaListItem) {

        }
    }
}