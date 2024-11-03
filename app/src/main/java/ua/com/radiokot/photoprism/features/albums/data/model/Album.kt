package ua.com.radiokot.photoprism.features.albums.data.model

import ua.com.radiokot.photoprism.api.albums.model.PhotoPrismAlbum
import ua.com.radiokot.photoprism.features.gallery.data.model.WithThumbnail
import ua.com.radiokot.photoprism.features.gallery.data.model.WithThumbnailFromUrlFactory
import ua.com.radiokot.photoprism.features.gallery.data.model.parsePhotoPrismDate
import ua.com.radiokot.photoprism.features.gallery.logic.MediaPreviewUrlFactory
import java.util.Date

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
    val description: String?,
    val path: String,
    val uid: String,
    val isFavorite: Boolean,
    /**
     * Creation date, but it should not be used to sort automatically created folders
     * like "December 2023". For those, see [ymd].
     * Makes sense for actual albums.
     */
    val createdAt: Date,
    /**
     * String representation of album's YYYYMMDD.
     * This "date" can be used to sort automatically created folders like "December 2023".
     * Makes no sense for actual albums.
     */
    val ymd: String,
    previewHash: String,
    previewUrlFactory: MediaPreviewUrlFactory,
) : WithThumbnail by WithThumbnailFromUrlFactory(previewHash, previewUrlFactory) {

    constructor(
        source: PhotoPrismAlbum,
        previewUrlFactory: MediaPreviewUrlFactory,
    ) : this(
        type = TypeName.fromPhotoPrismType(source.type),
        title = source.title,
        description = source.description.takeIf(String::isNotEmpty),
        path = source.path,
        uid = source.uid,
        isFavorite = source.favorite,
        createdAt = parsePhotoPrismDate(source.createdAt)!!,
        ymd = "%04d%02d%02d".format(source.year, source.month, source.day),
        previewHash = source.thumb,
        previewUrlFactory = previewUrlFactory,
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

    enum class TypeName(val value: String) { //all types of Album
        ALBUM("album"), //order by favorites
        FOLDER("folder"), //order by name
        MOMENT("moment"), //order by newest
        MONTH("month"), //order by newest
        ;

        companion object {
            fun fromPhotoPrismType(type: String): TypeName {
                return when (type) {
                    ALBUM.value -> ALBUM
                    FOLDER.value -> FOLDER
                    MOMENT.value -> MOMENT
                    MONTH.value -> MONTH
                    else -> error("Unsupported PhotoPrism media type '$type'")
                }
            }
        }
    }
}
