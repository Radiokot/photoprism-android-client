package ua.com.radiokot.photoprism.features.importt.albums.view.model

import android.view.View
import android.widget.TextView
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.items.AbstractItem
import ua.com.radiokot.photoprism.R
import ua.com.radiokot.photoprism.databinding.ListItemImportAlbumBinding
import ua.com.radiokot.photoprism.features.importt.albums.data.model.ImportAlbum
import ua.com.radiokot.photoprism.features.importt.albums.view.ImportAlbumListItemDiffCallback

sealed class ImportAlbumListItem : AbstractItem<FastAdapter.ViewHolder<out ImportAlbumListItem>>() {

    /**
     * Do not forget to update [ImportAlbumListItemDiffCallback]
     * when changing fields.
     */

    class CreateNew(
        val newAlbumTitle: String,
    ) : ImportAlbumListItem() {

        override var identifier: Long =
            "create_new".hashCode().toLong()

        override val layoutRes: Int =
            R.layout.list_item_import_album_create_new

        override val type: Int =
            R.layout.list_item_import_album_create_new

        override fun getViewHolder(v: View) =
            ViewHolder(v)

        class ViewHolder(itemView: View) : FastAdapter.ViewHolder<CreateNew>(itemView) {
            private val titleTextView = itemView as TextView

            override fun bindView(item: CreateNew, payloads: List<Any>) {
                titleTextView.text = titleTextView.context.getString(
                    R.string.template_import_album_create_new,
                    item.newAlbumTitle
                )
            }

            override fun unbindView(item: CreateNew) {
                // No special handling is needed.
            }

            companion object {
                const val PAYLOAD_TITLE_CHANGED = "title"
            }
        }
    }

    class Album(
        val title: String,
        val isAlbumSelected: Boolean,
        val source: ImportAlbum?,
    ) : ImportAlbumListItem() {

        override var identifier: Long =
            source?.hashCode()?.toLong() ?: -1L

        override val layoutRes: Int =
            R.layout.list_item_import_album

        override val type: Int =
            R.layout.list_item_import_album

        constructor(
            source: ImportAlbum,
            isAlbumSelected: Boolean,
        ) : this(
            title = source.title,
            isAlbumSelected = isAlbumSelected,
            source = source,
        )

        override fun getViewHolder(v: View) =
            ViewHolder(v)

        class ViewHolder(itemView: View) : FastAdapter.ViewHolder<Album>(itemView) {
            private val view = ListItemImportAlbumBinding.bind(itemView)

            init {
                // Dispatch selection checkbox clicks to the root view
                // and prevent them from changing the checkbox selection state
                // by disabling isClickable – although it is disabled,
                // the click listener is called anyway.
                // isClickable must only be disabled after setting the listener.
                view.selectionCheckBox.setOnClickListener {
                    view.root.callOnClick()
                }
                view.selectionCheckBox.isClickable = false
            }

            override fun bindView(item: Album, payloads: List<Any>) = with(view) {
                titleTextView.text = item.title
                selectionCheckBox.isChecked = item.isAlbumSelected
            }

            override fun unbindView(item: Album) {
                // No special handling is needed.
            }

            companion object {
                const val PAYLOAD_SELECTION_CHANGED = "selection"
            }
        }
    }
}
