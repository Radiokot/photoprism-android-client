package ua.com.radiokot.photoprism.features.shared.albums.view.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import ua.com.radiokot.photoprism.features.shared.albums.data.model.Album

@Parcelize
data class AlbumSort(
    val order: Order,
    val areFavoritesFirst: Boolean,
) : Parcelable,
    Comparator<Album> {

    override fun compare(a1: Album, a2: Album): Int {
        // As albums have different types, let's separate them first.
        val comparedByType = a1.type.compareTo(a2.type)
        if (comparedByType != 0) {
            return comparedByType
        }

        if (areFavoritesFirst) {
            val comparedByFavorite = a2.isFavorite.compareTo(a1.isFavorite)
            if (comparedByFavorite != 0) {
                return comparedByFavorite
            }
        }

        val comparedByOrder = when (order) {
            Order.NAME ->
                if (a1.path.isNotEmpty() && a2.path.isNotEmpty())
                    a1.path.compareTo(a2.path)
                else
                    a1.title.compareTo(a2.title)

            Order.NAME_DESC ->
                // This is how PhotoPrism "Name" works,
                // considering albums are not compared to folders at this point.
                if (a1.path.isNotEmpty() && a2.path.isNotEmpty())
                    a2.path.compareTo(a1.path)
                else
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

        return comparedByOrder
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
