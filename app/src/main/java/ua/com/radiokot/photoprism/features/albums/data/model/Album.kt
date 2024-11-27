package ua.com.radiokot.photoprism.features.albums.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import ua.com.radiokot.photoprism.api.albums.model.PhotoPrismAlbum
import ua.com.radiokot.photoprism.extension.getUtcCalendar
import ua.com.radiokot.photoprism.util.LocalDate
import java.util.Calendar

class Album(
    /*
    Query example:
    albums?count=24&offset=0&q=&category=&order=name&year=&type=$TypeName
    Only the order change:
    - Requesting "album" type to album photoprism order it by "favorites"
    - Requesting "folder" type to album photoprism order it by "name", etc etc
     */
    val type: TypeName,
    val title: String, //same as photos name,
    val path: String?,
    val uid: String,
    val isFavorite: Boolean,
    /**
     * String representation of album's YYYYMMDD.
     * This "date" can be used to sort automatically created folders like "December 2023".
     * Makes no sense for actual albums.
     */
    val ymd: String,
    val thumbnailHash: String,
) {
    val ymdLocalDate: LocalDate
        get() =
            getUtcCalendar().let {
                it[Calendar.DAY_OF_MONTH] = ymd.substring(6..7).toInt().coerceAtLeast(1)
                it[Calendar.YEAR] = ymd.substring(0..3).toInt()
                it[Calendar.MONTH] = ymd.substring(4..5).toInt() - 1
                LocalDate(it.time)
            }

    constructor(
        source: PhotoPrismAlbum,
    ) : this(
        type = TypeName.fromPhotoPrismType(source.type),
        title = source.title,
        path = source.path?.takeIf(String::isNotEmpty),
        uid = source.uid,
        isFavorite = source.favorite,
        ymd = "%04d%02d%02d".format(source.year, source.month, source.day),
        thumbnailHash = source.thumb,
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Album

        if (uid != other.uid) return false

        return true
    }

    override fun hashCode(): Int {
        return uid.hashCode()
    }

    override fun toString(): String {
        return "Album(uid='$uid', title='$title', type=$type)"
    }

    @Parcelize
    enum class TypeName(val value: String) : Parcelable {
        ALBUM("album"),
        FOLDER("folder"),
        MONTH("month"),
        ;

        companion object {
            fun fromPhotoPrismType(type: String): TypeName {
                return when (type) {
                    ALBUM.value -> ALBUM
                    FOLDER.value -> FOLDER
                    MONTH.value -> MONTH
                    else -> error("Unsupported PhotoPrism media type '$type'")
                }
            }
        }
    }

    companion object {
        const val YMD_UNSPECIFIED = "00000000"
    }
}
