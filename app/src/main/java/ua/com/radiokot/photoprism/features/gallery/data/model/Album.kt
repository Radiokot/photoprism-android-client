package ua.com.radiokot.photoprism.features.gallery.data.model

import ua.com.radiokot.photoprism.api.albums.model.PhotoPrismAlbum
import ua.com.radiokot.photoprism.api.model.PhotoPrismOrder
import ua.com.radiokot.photoprism.features.gallery.logic.MediaPreviewUrlFactory
import java.util.*

class Album (
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
    val createdAt: Date,
    val smallThumbnailUrl: String,
) {

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
        return "Album(uid='$uid', type=$type)"
    }

    enum class TypeName(val value: String) { //all types of Album
        ALBUM("album"), //order by favorites
        FOLDER("folder"), //order by name
        MOMENT("moment"), //order by newest
        MONTH("month"), //order by newest
        //STATE("state"),
        ;
    }

    sealed class TypeData(val typeName: TypeName) {
        interface order {
            val orderBy: PhotoPrismOrder
        }
        interface ViewableAsImage {
            val hdPreviewUrl: String
        }

        //object Unknown : TypeData(GalleryMedia.TypeName.UNKNOWN)

        class Album(
            override val orderBy: PhotoPrismOrder,
            override val hdPreviewUrl: String,
        ) : TypeData(TypeName.ALBUM), order, ViewableAsImage

        class Folder(
            override val orderBy: PhotoPrismOrder,
            override val hdPreviewUrl: String,
        ) : TypeData(TypeName.FOLDER), order, ViewableAsImage

        class Moment(
            override val orderBy: PhotoPrismOrder,
            override val hdPreviewUrl: String,
        ) : TypeData(TypeName.MOMENT), order, ViewableAsImage

        class Month(
            override val orderBy: PhotoPrismOrder,
            override val hdPreviewUrl: String,
        ) : TypeData(TypeName.MONTH), order, ViewableAsImage

        /*
        class State(
            override val avcPreviewUrl: String,
        ) : TypeData(Album.TypeName.STATE), ViewableAsVideo.
         */

        companion object {
            fun fromPhotoPrism(
                source: PhotoPrismAlbum,
                previewUrlFactory: MediaPreviewUrlFactory,
            ): TypeData =
                when (val type = source.type) {
                    TypeName.ALBUM.value -> Album(
                        orderBy = PhotoPrismOrder.FAVORITES,
                        hdPreviewUrl = previewUrlFactory.getHdPreviewUrl(source.thumb),
                    )
                    TypeName.FOLDER.value -> Folder(
                        orderBy = PhotoPrismOrder.NAME,
                        hdPreviewUrl = previewUrlFactory.getHdPreviewUrl(source.thumb),
                    )
                    TypeName.MOMENT.value -> Moment(
                        orderBy = PhotoPrismOrder.NEWEST,
                        hdPreviewUrl = previewUrlFactory.getHdPreviewUrl(source.thumb),
                    )
                    TypeName.MONTH.value -> Month(
                        orderBy = PhotoPrismOrder.NEWEST,
                        hdPreviewUrl = previewUrlFactory.getHdPreviewUrl(source.thumb),
                    )
                    /*
                    TypeName.STATE.value -> Video(
                        orderBy = PhotoPrismOrder.PLACE.toString(),
                        hdPreviewUrl = previewUrlFactory.getHdPreviewUrl(source.thumb),
                    )
                     */
                    else -> error("Unsupported PhotoPrism album type '$type'")
                }
        }
    }
}