package ua.com.radiokot.photoprism.features.shared.albums.view.model

import androidx.annotation.StringRes
import ua.com.radiokot.photoprism.R

object AlbumSortResources {
    @StringRes
    fun getName(order: AlbumSort.Order): Int = when (order) {
        AlbumSort.Order.NAME ->
            R.string.sort_order_name

        AlbumSort.Order.NAME_DESC ->
            R.string.sort_order_name_desc

        AlbumSort.Order.NEWEST_FIRST ->
            R.string.sort_order_newest_first

        AlbumSort.Order.OLDEST_FIRST ->
            R.string.sort_order_oldest_first

        AlbumSort.Order.RECENTLY_ADDED ->
            R.string.sort_order_recently_added
    }
}
