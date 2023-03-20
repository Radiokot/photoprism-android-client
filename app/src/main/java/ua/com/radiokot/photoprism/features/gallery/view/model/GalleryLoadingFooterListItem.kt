package ua.com.radiokot.photoprism.features.gallery.view.model

import android.view.View
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

    override var identifier: Long
        get() {
            var r = 1L
            r = 31 * r + isLoading.hashCode()
            r = 31 * r + canLoadMore.hashCode()
            return r
        }
        set(_) {
            error("Do not rewrite my value")
        }


    override fun getViewHolder(v: View): ViewHolder =
        ViewHolder(v)

    class ViewHolder(itemView: View) :
        FastAdapter.ViewHolder<GalleryLoadingFooterListItem>(itemView) {
        val view = ListItemGalleryLoadingFooterBinding.bind(itemView)

        override fun bindView(item: GalleryLoadingFooterListItem, payloads: List<Any>) {
            with(view) {
                if (item.isLoading) {
                    progressIndicator.visibility = View.VISIBLE
                    loadMoreButton.visibility = View.GONE
                } else {
                    progressIndicator.visibility = View.GONE
                    if (item.canLoadMore) {
                        loadMoreButton.visibility = View.VISIBLE
                    } else {
                        loadMoreButton.visibility = View.GONE
                    }
                }
            }
        }

        override fun unbindView(item: GalleryLoadingFooterListItem) {}
    }
}