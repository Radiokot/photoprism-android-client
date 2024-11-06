package ua.com.radiokot.photoprism.features.albums.view.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import ua.com.radiokot.photoprism.features.albums.data.model.Album

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
            // https://github.com/photoprism/photoprism/blob/f7e5b7b9207205be61b3d9aac70990c57a9ddcd1/internal/entity/search/albums.go#L118
            Order.NAME ->
                if (a1.path != null && a2.path != null)
                    a1.path.compareTo(a2.path)
                else
                    a1.title.compareTo(a2.title)

            // https://github.com/photoprism/photoprism/blob/f7e5b7b9207205be61b3d9aac70990c57a9ddcd1/internal/entity/search/albums.go#L118
            Order.NAME_DESC ->
                if (a1.path != null && a2.path != null)
                    a2.path.compareTo(a1.path)
                else
                    a2.title.compareTo(a1.title)

            // https://github.com/photoprism/photoprism/blob/f7e5b7b9207205be61b3d9aac70990c57a9ddcd1/internal/entity/search/albums.go#L82
            Order.NEWEST_FIRST ->
                if (a1.ymd != Album.YMD_UNSPECIFIED && a2.ymd != Album.YMD_UNSPECIFIED)
                    a2.ymd.compareTo(a1.ymd)
                else
                    a2.uid.compareTo(a1.uid)

            // https://github.com/photoprism/photoprism/blob/f7e5b7b9207205be61b3d9aac70990c57a9ddcd1/internal/entity/search/albums.go#L90
            Order.OLDEST_FIRST ->
                if (a1.ymd != Album.YMD_UNSPECIFIED && a2.ymd != Album.YMD_UNSPECIFIED)
                    a1.ymd.compareTo(a2.ymd)
                else
                    a1.uid.compareTo(a2.uid)

            // https://github.com/photoprism/photoprism/blob/f7e5b7b9207205be61b3d9aac70990c57a9ddcd1/internal/entity/search/albums.go#L98
            Order.RECENTLY_ADDED ->
                a2.uid.compareTo(a1.uid)
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
