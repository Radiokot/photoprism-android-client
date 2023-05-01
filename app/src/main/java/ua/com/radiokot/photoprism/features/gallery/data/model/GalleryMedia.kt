package ua.com.radiokot.photoprism.features.gallery.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import ua.com.radiokot.photoprism.api.photos.model.PhotoPrismMergedPhoto
import ua.com.radiokot.photoprism.features.gallery.logic.MediaFileDownloadUrlFactory
import ua.com.radiokot.photoprism.features.gallery.logic.MediaPreviewUrlFactory
import ua.com.radiokot.photoprism.features.gallery.logic.MediaWebUrlFactory
import java.util.*

class GalleryMedia(
    val media: TypeData,
    val uid: String,
    val width: Int,
    val height: Int,
    val takenAt: Date,
    val name: String,
    val smallThumbnailUrl: String,
    val webViewUrl: String,
    files: List<File>,
) {
    var files: List<File> = files
        private set

    constructor(
        source: PhotoPrismMergedPhoto,
        previewUrlFactory: MediaPreviewUrlFactory,
        downloadUrlFactory: MediaFileDownloadUrlFactory,
        webUrlFactory: MediaWebUrlFactory,
    ) : this(
        media = TypeData.fromPhotoPrism(
            source = source,
            previewUrlFactory = previewUrlFactory
        ),
        uid = source.uid,
        width = source.width,
        height = source.height,
        takenAt = parsePhotoPrismDate(source.takenAt)!!,
        name = source.name,
        smallThumbnailUrl = previewUrlFactory.getSmallThumbnailUrl(source.hash),
        webViewUrl = webUrlFactory.getWebViewUrl(source.uid),
        files = source.files.map { photoPrismFile ->
            File(
                source = photoPrismFile,
                thumbnailUrlFactory = previewUrlFactory,
                downloadUrlFactory = downloadUrlFactory,
            )
        }.toMutableList()
    )

    /**
     * Merges current [files] with [moreFiles] overwriting the value
     */
    fun mergeFiles(moreFiles: Collection<File>) {
        files = (files + moreFiles).distinct()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as GalleryMedia

        if (uid != other.uid) return false

        return true
    }

    override fun hashCode(): Int {
        return uid.hashCode()
    }

    override fun toString(): String {
        return "GalleryMedia(uid='$uid', media=$media)"
    }


    /**
    photoprism/pkg/media/types.go

    const (
    Unknown  Type = ""
    Image    Type = "image"
    Raw      Type = "raw"
    Animated Type = "animated"
    Live     Type = "live"
    Video    Type = "video"
    Vector   Type = "vector"
    Sidecar  Type = "sidecar"
    Text     Type = "text"
    Other    Type = "other"
    )
     */
    enum class TypeName(val value: String) {
        UNKNOWN(""),
        IMAGE("image"),
        RAW("raw"),
        ANIMATED("animated"),
        LIVE("live"),
        VIDEO("video"),
        VECTOR("vector"),
        SIDECAR("sidecar"),
        TEXT("text"),
        OTHER("other"),
        ;
    }

    sealed class TypeData(val typeName: TypeName) {
        interface ViewableAsImage {
            val hdPreviewUrl: String
        }

        interface ViewableAsVideo {
            val avcPreviewUrl: String
        }

        object Unknown : TypeData(TypeName.UNKNOWN)

        class Image(
            override val hdPreviewUrl: String,
        ) : TypeData(TypeName.IMAGE), ViewableAsImage

        class Raw(
            override val hdPreviewUrl: String,
        ) : TypeData(TypeName.RAW), ViewableAsImage

        class Animated(
            override val avcPreviewUrl: String,
        ) : TypeData(TypeName.ANIMATED), ViewableAsVideo

        class Live(
            override val avcPreviewUrl: String,
        ) : TypeData(TypeName.LIVE), ViewableAsVideo

        class Video(
            override val avcPreviewUrl: String,
        ) : TypeData(TypeName.VIDEO), ViewableAsVideo

        class Vector(
            override val hdPreviewUrl: String,
        ) : TypeData(TypeName.VECTOR), ViewableAsImage

        object Sidecar : TypeData(TypeName.SIDECAR)
        object Text : TypeData(TypeName.TEXT)
        object Other : TypeData(TypeName.OTHER)

        companion object {
            fun fromPhotoPrism(
                source: PhotoPrismMergedPhoto,
                previewUrlFactory: MediaPreviewUrlFactory,
            ): TypeData =
                when (val type = source.type) {
                    TypeName.UNKNOWN.value -> Unknown
                    TypeName.IMAGE.value -> Image(
                        hdPreviewUrl = previewUrlFactory.getHdPreviewUrl(source.hash),
                    )
                    TypeName.RAW.value -> Raw(
                        hdPreviewUrl = previewUrlFactory.getHdPreviewUrl(source.hash),
                    )
                    TypeName.ANIMATED.value -> Animated(
                        avcPreviewUrl = previewUrlFactory.getMp4PreviewUrl(source.hash),
                    )
                    TypeName.LIVE.value -> Live(
                        avcPreviewUrl = previewUrlFactory.getMp4PreviewUrl(source.hash),
                    )
                    TypeName.VIDEO.value -> Video(
                        avcPreviewUrl = previewUrlFactory.getMp4PreviewUrl(source.hash),
                    )
                    TypeName.VECTOR.value -> Vector(
                        hdPreviewUrl = previewUrlFactory.getHdPreviewUrl(source.hash),
                    )
                    TypeName.SIDECAR.value -> Sidecar
                    TypeName.TEXT.value -> Text
                    TypeName.OTHER.value -> Other
                    else -> error("Unsupported PhotoPrism media type '$type'")
                }
        }
    }

    @Parcelize
    class File(
        val name: String,
        val uid: String,
        val mimeType: String,
        val sizeBytes: Long,
        val thumbnailUrlSmall: String,
        val downloadUrl: String,
    ) : Parcelable {
        constructor(
            source: PhotoPrismMergedPhoto.File,
            thumbnailUrlFactory: MediaPreviewUrlFactory,
            downloadUrlFactory: MediaFileDownloadUrlFactory,
        ) : this(
            name = source.name,
            uid = source.uid,
            mimeType = source.mime,
            sizeBytes = source.size,
            thumbnailUrlSmall = thumbnailUrlFactory.getSmallThumbnailUrl(source.hash),
            downloadUrl = downloadUrlFactory.getDownloadUrl(source.hash),
        )

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as File

            if (uid != other.uid) return false

            return true
        }

        override fun hashCode(): Int {
            return uid.hashCode()
        }

        override fun toString(): String {
            return "File(uid='$uid', name='$name')"
        }
    }

    companion object {
        fun parsePhotoPrismDate(date: String): Date? = synchronized(photoPrismDateFormat) {
            photoPrismDateFormat.parse(date)
        }
    }
}