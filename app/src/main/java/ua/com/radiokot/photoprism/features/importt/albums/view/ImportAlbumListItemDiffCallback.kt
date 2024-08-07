package ua.com.radiokot.photoprism.features.importt.albums.view

import com.mikepenz.fastadapter.diff.DiffCallback
import ua.com.radiokot.photoprism.features.importt.albums.view.model.ImportAlbumListItem

class ImportAlbumListItemDiffCallback : DiffCallback<ImportAlbumListItem> {
    override fun areItemsTheSame(
        oldItem: ImportAlbumListItem,
        newItem: ImportAlbumListItem
    ): Boolean =
        (oldItem is ImportAlbumListItem.Album && newItem is ImportAlbumListItem.Album
                && oldItem.identifier == newItem.identifier)
                || oldItem is ImportAlbumListItem.CreateNew && newItem is ImportAlbumListItem.CreateNew

    override fun areContentsTheSame(
        oldItem: ImportAlbumListItem,
        newItem: ImportAlbumListItem
    ): Boolean =
        when {
            oldItem is ImportAlbumListItem.Album && newItem is ImportAlbumListItem.Album ->
                oldItem.isAlbumSelected == newItem.isAlbumSelected
                        && oldItem.title == newItem.title

            oldItem is ImportAlbumListItem.CreateNew && newItem is ImportAlbumListItem.CreateNew ->
                oldItem.newAlbumTitle == newItem.newAlbumTitle

            else ->
                false
        }

    override fun getChangePayload(
        oldItem: ImportAlbumListItem,
        oldItemPosition: Int,
        newItem: ImportAlbumListItem,
        newItemPosition: Int
    ): Any? {
        if (oldItem is ImportAlbumListItem.Album && newItem is ImportAlbumListItem.Album
            && oldItem.isAlbumSelected != newItem.isAlbumSelected
        ) {
            return ImportAlbumListItem.Album.ViewHolder.PAYLOAD_SELECTION_CHANGED
        } else if (oldItem is ImportAlbumListItem.CreateNew && newItem is ImportAlbumListItem.CreateNew
            && oldItem.newAlbumTitle != newItem.newAlbumTitle
        ) {
            return ImportAlbumListItem.CreateNew.ViewHolder.PAYLOAD_TITLE_CHANGED
        }
        return null
    }
}
