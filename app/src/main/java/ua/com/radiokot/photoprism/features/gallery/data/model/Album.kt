package ua.com.radiokot.photoprism.features.gallery.data.model

import ua.com.radiokot.photoprism.api.albums.model.PhotoPrismAlbum
import ua.com.radiokot.photoprism.features.gallery.logic.MediaPreviewUrlFactory

class Album(
    /*
    Query example:
    albums?count=24&offset=0&q=&category=&order=name&year=&type=$TypeName
    Only the order change:
    - Requesting "album" type to album photoprism order it by "favorites"
    - Requesting "folder" type to album photoprism order it by "name", etc etc
     */
    val type: TypeName,
    val title: String, //same as photos name
    val uid: String,
    val smallThumbnailUrl: String,
) {

    constructor(
        source: PhotoPrismAlbum,
        previewUrlFactory: MediaPreviewUrlFactory,
    ) : this(
        type = TypeName.fromPhotoPrismType(source.type),
        title = source.title,
        uid = source.uid,
        smallThumbnailUrl = previewUrlFactory.getSmallThumbnailUrl(source.thumb),
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