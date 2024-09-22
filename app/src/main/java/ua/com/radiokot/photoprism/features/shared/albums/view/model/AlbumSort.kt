package ua.com.radiokot.photoprism.features.shared.albums.view.model

import ua.com.radiokot.photoprism.features.shared.albums.data.model.Album
import java.io.Serializable

data class AlbumSort(
    val order: Order,
    val areFavoritesFirst: Boolean,
) : Serializable,
    Comparator<Album> {

    val comparator: Comparator<Album>
        get() = when (order) {
            Order.NAME ->
                compareBy(Album::title)

            Order.NAME_DESC ->
                compareByDescending(Album::title)

            Order.NEWEST_FIRST ->
                compareByDescending(Album::createdAt)

            Order.OLDEST_FIRST ->
                compareBy(Album::createdAt)

            Order.RECENTLY_UPDATED ->
                compareByDescending(Album::updatedAt)
        }.let { orderComparator ->
            if (areFavoritesFirst) {
                compareByDescending(Album::isFavorite)
                    .then(orderComparator)
            } else {
                orderComparator
            }
        }

    override fun compare(a1: Album, a2: Album): Int {
        val comparedByOrder = when (order) {
            Order.NAME ->
                a1.title.compareTo(a2.title)

            Order.NAME_DESC ->
                a2.title.compareTo(a1.title)

            Order.NEWEST_FIRST ->
                a2.createdAt.compareTo(a1.createdAt)

            Order.OLDEST_FIRST ->
                a1.createdAt.compareTo(a2.createdAt)

            Order.RECENTLY_UPDATED ->
                a2.updatedAt.compareTo(a1.updatedAt)
        }

        return if (areFavoritesFirst) {
            val comparedByFavorite = a2.isFavorite.compareTo(a1.isFavorite)
            if (comparedByFavorite != 0) {
                comparedByFavorite
            } else {
                comparedByOrder
            }
        } else {
            comparedByOrder
        }
    }

    enum class Order {
        NAME,
        NAME_DESC,
        NEWEST_FIRST,
        OLDEST_FIRST,
        RECENTLY_UPDATED,
        ;
    }
}
