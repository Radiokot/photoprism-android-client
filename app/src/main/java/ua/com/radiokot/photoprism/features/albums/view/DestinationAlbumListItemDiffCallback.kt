package ua.com.radiokot.photoprism.features.albums.view

import com.mikepenz.fastadapter.diff.DiffCallback
import ua.com.radiokot.photoprism.features.albums.view.model.DestinationAlbumListItem

class DestinationAlbumListItemDiffCallback : DiffCallback<DestinationAlbumListItem> {
    override fun areItemsTheSame(
        oldItem: DestinationAlbumListItem,
        newItem: DestinationAlbumListItem
    ): Boolean =
        (oldItem is DestinationAlbumListItem.Album && newItem is DestinationAlbumListItem.Album
                && oldItem.identifier == newItem.identifier)
                || oldItem is DestinationAlbumListItem.CreateNew && newItem is DestinationAlbumListItem.CreateNew

    override fun areContentsTheSame(
        oldItem: DestinationAlbumListItem,
        newItem: DestinationAlbumListItem
    ): Boolean =
        when {
            oldItem is DestinationAlbumListItem.Album && newItem is DestinationAlbumListItem.Album ->
                oldItem.isAlbumSelected == newItem.isAlbumSelected
                        && oldItem.title == newItem.title

            oldItem is DestinationAlbumListItem.CreateNew && newItem is DestinationAlbumListItem.CreateNew ->
                oldItem.newAlbumTitle == newItem.newAlbumTitle

            else ->
                false
        }

    override fun getChangePayload(
        oldItem: DestinationAlbumListItem,
        oldItemPosition: Int,
        newItem: DestinationAlbumListItem,
        newItemPosition: Int
    ): Any? {
        if (oldItem is DestinationAlbumListItem.Album && newItem is DestinationAlbumListItem.Album
            && oldItem.isAlbumSelected != newItem.isAlbumSelected
        ) {
            return DestinationAlbumListItem.Album.ViewHolder.PAYLOAD_SELECTION_CHANGED
        } else if (oldItem is DestinationAlbumListItem.CreateNew && newItem is DestinationAlbumListItem.CreateNew
            && oldItem.newAlbumTitle != newItem.newAlbumTitle
        ) {
            return DestinationAlbumListItem.CreateNew.ViewHolder.PAYLOAD_TITLE_CHANGED
        }
        return null
    }
}
