package ua.com.radiokot.photoprism.features.gallery.view.model

import android.view.View
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.items.AbstractItem
import ua.com.radiokot.photoprism.R
import ua.com.radiokot.photoprism.features.gallery.data.model.Album

class AlbumListItem(
    val title: String,
    val thumbnailUrl: String,
    // TODO: Selection state
    val source: Album?,
) : AbstractItem<AlbumListItem.ViewHolder>() {
    override val layoutRes: Int =
        R.layout.list_item_album

    override val type: Int =
        R.id.list_item_album

    override var identifier: Long =
        source?.hashCode()?.toLong() ?: -1L

    constructor(source: Album) : this(
        title = source.title,
        thumbnailUrl = source.smallThumbnailUrl,
        source = source,
    )

    override fun getViewHolder(v: View): ViewHolder =
        ViewHolder(v)

    class ViewHolder(itemView: View) :
        FastAdapter.ViewHolder<AlbumListItem>(itemView) {
        override fun bindView(item: AlbumListItem, payloads: List<Any>) {
            TODO("Not yet implemented")
        }

        override fun unbindView(item: AlbumListItem) {
            TODO("Not yet implemented")
        }

    }
}