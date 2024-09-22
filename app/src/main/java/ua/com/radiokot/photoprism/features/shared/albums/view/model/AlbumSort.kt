package ua.com.radiokot.photoprism.features.shared.albums.view.model

import ua.com.radiokot.photoprism.features.shared.albums.data.model.Album
import java.io.Serializable

data class AlbumSort(
    val order: Order,
    val areFavoritesFirst: Boolean,
) : Serializable,
    Comparator<Album> {

    override fun compare(a1: Album, a2: Album): Int {
        val comparedByOrder = when (order) {
            Order.NAME ->
                a1.title.compareTo(a2.title)

            Order.NAME_DESC ->
                a2.title.compareTo(a1.title)

            Order.NEWEST_FIRST ->
                // This is how PhotoPrism "Newest first" works.
                a2.ymd.compareTo(a1.ymd)

            Order.OLDEST_FIRST ->
                // See above.
                a1.ymd.compareTo(a2.ymd)

            Order.RECENTLY_ADDED ->
                a2.createdAt.compareTo(a1.createdAt)
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
        RECENTLY_ADDED,
        ;
    }
}
