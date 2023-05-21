package ua.com.radiokot.photoprism.features.gallery.view

import com.mikepenz.fastadapter.diff.DiffCallback
import ua.com.radiokot.photoprism.features.gallery.view.model.GalleryListItem

class GalleryListItemDiffCallback : DiffCallback<GalleryListItem> {
    override fun areItemsTheSame(oldItem: GalleryListItem, newItem: GalleryListItem): Boolean =
        oldItem.identifier == newItem.identifier

    override fun areContentsTheSame(oldItem: GalleryListItem, newItem: GalleryListItem): Boolean =
        if (oldItem is GalleryListItem.Media && newItem is GalleryListItem.Media)
            oldItem.isMediaSelected == newItem.isMediaSelected
                    && oldItem.isSelectionViewVisible == newItem.isSelectionViewVisible
                    && oldItem.isViewButtonVisible == newItem.isViewButtonVisible
        else
            true

    override fun getChangePayload(
        oldItem: GalleryListItem,
        oldItemPosition: Int,
        newItem: GalleryListItem,
        newItemPosition: Int
    ): Any? {
        if (oldItem is GalleryListItem.Media && newItem is GalleryListItem.Media
            && oldItem.isMediaSelected != newItem.isMediaSelected
        ) {
            return GalleryListItem.Media.ViewHolder.PAYLOAD_ANIMATE_SELECTION
        }
        return null
    }
}