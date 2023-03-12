package ua.com.radiokot.photoprism.features.gallery.view.model

import android.view.View
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.items.AbstractItem
import ua.com.radiokot.photoprism.R

class GalleryProgressListItem : AbstractItem<GalleryProgressListItem.ViewHolder>() {
    override val type: Int
        get() = R.id.list_item_gallery_progress

    override val layoutRes: Int
        get() = R.layout.list_item_gallery_progress

    override fun getViewHolder(v: View): ViewHolder =
        ViewHolder(v)

    class ViewHolder(itemView: View) : FastAdapter.ViewHolder<GalleryProgressListItem>(itemView) {
        override fun bindView(item: GalleryProgressListItem, payloads: List<Any>) {}

        override fun unbindView(item: GalleryProgressListItem) {}
    }
}