package ua.com.radiokot.photoprism.features.gallery.folders.view.model

import androidx.annotation.StringRes
import ua.com.radiokot.photoprism.R

object GalleryFolderOrderResources {
    @StringRes
    fun getName(order: GalleryFolderOrder): Int = when (order) {
        GalleryFolderOrder.NAME ->
            R.string.sort_order_name

        GalleryFolderOrder.NAME_DESC ->
            R.string.sort_order_name_desc

        GalleryFolderOrder.NEWEST_FIRST ->
            R.string.sort_order_newest_first

        GalleryFolderOrder.OLDEST_FIRST ->
            R.string.sort_order_oldest_first

        GalleryFolderOrder.RECENTLY_UPDATED ->
            R.string.sort_order_recently_updated
    }
}
