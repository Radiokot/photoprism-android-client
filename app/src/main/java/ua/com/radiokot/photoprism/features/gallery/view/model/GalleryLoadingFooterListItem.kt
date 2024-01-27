package ua.com.radiokot.photoprism.features.gallery.view.model

import android.view.View
import androidx.core.view.isInvisible
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.items.AbstractItem
import ua.com.radiokot.photoprism.R
import ua.com.radiokot.photoprism.databinding.ListItemGalleryLoadingFooterBinding

class GalleryLoadingFooterListItem(
    val isLoading: Boolean,
    val canLoadMore: Boolean,
) : AbstractItem<GalleryLoadingFooterListItem.ViewHolder>() {
    override val type: Int
        get() = R.id.list_item_gallery_loading_footer

    override val layoutRes: Int
        get() = R.layout.list_item_gallery_loading_footer

    override var identifier: Long =
        "loading".hashCode().toLong()

    override fun getViewHolder(v: View): ViewHolder =
        ViewHolder(v)

    class ViewHolder(itemView: View) :
        FastAdapter.ViewHolder<GalleryLoadingFooterListItem>(itemView) {
        val view = ListItemGalleryLoadingFooterBinding.bind(itemView)

        override fun bindView(item: GalleryLoadingFooterListItem, payloads: List<Any>) {
            with(view) {
                // Invisibility is used prevent unwanted vertical movement of the list items
                // due to the footer size change or adding/removal.
                progressIndicator.isInvisible = !item.isLoading
                loadMoreButton.isInvisible = item.isLoading || !item.canLoadMore
            }
        }

        override fun unbindView(item: GalleryLoadingFooterListItem) {}
    }
}
