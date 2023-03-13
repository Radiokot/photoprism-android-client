package ua.com.radiokot.photoprism.features.viewer.view.model

import android.view.View
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.items.AbstractItem
import com.squareup.picasso.Picasso
import ua.com.radiokot.photoprism.R
import ua.com.radiokot.photoprism.databinding.PagerItemMediaViewerBinding
import ua.com.radiokot.photoprism.features.gallery.data.model.GalleryMedia

class MediaViewerPageItem(
    val imageUrl: String,
) : AbstractItem<MediaViewerPageItem.ViewHolder>() {

    constructor(source: GalleryMedia) : this(
        imageUrl = source.smallThumbnailUrl,
    )

    override val type: Int
        get() = R.id.pager_item_media_viewer

    override val layoutRes: Int
        get() = R.layout.pager_item_media_viewer

    override fun getViewHolder(v: View): ViewHolder =
        ViewHolder(v)

    class ViewHolder(itemView: View) : FastAdapter.ViewHolder<MediaViewerPageItem>(itemView) {
        private val view = PagerItemMediaViewerBinding.bind(itemView)

        override fun bindView(item: MediaViewerPageItem, payloads: List<Any>) {
            Picasso.get()
                .load(item.imageUrl)
                .into(view.photoView)
        }

        override fun unbindView(item: MediaViewerPageItem) {
            Picasso.get().cancelRequest(view.photoView)
        }
    }
}