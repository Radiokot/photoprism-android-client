package ua.com.radiokot.photoprism.features.viewer.view.model

import android.view.View
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.items.AbstractItem
import ua.com.radiokot.photoprism.R
import ua.com.radiokot.photoprism.databinding.PagerItemMediaViewerBinding

class MediaViewerPageItem(
    val index: Int,
) : AbstractItem<MediaViewerPageItem.ViewHolder>() {
    override val type: Int
        get() = R.id.pager_item_media_viewer

    override val layoutRes: Int
        get() = R.layout.pager_item_media_viewer

    override fun getViewHolder(v: View): ViewHolder =
        ViewHolder(v)

    class ViewHolder(itemView: View) : FastAdapter.ViewHolder<MediaViewerPageItem>(itemView) {
        private val view = PagerItemMediaViewerBinding.bind(itemView)

        override fun bindView(item: MediaViewerPageItem, payloads: List<Any>) {
            view.mediaIndexTextView.text = item.index.toString()
        }

        override fun unbindView(item: MediaViewerPageItem) {
        }
    }
}