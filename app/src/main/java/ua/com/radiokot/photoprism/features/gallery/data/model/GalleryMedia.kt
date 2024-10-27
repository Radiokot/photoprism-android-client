package ua.com.radiokot.photoprism.features.gallery.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import ua.com.radiokot.photoprism.api.photos.model.PhotoPrismMergedPhoto
import ua.com.radiokot.photoprism.features.gallery.logic.MediaFileDownloadUrlFactory
import ua.com.radiokot.photoprism.features.gallery.logic.MediaPreviewUrlFactory
import ua.com.radiokot.photoprism.features.gallery.logic.MediaWebUrlFactory
import ua.com.radiokot.photoprism.util.LocalDate
import java.util.concurrent.TimeUnit

/**
 * A merged gallery media entry.
 */
class GalleryMedia(
    /**
     * Type-specific media data.
     */
    val media: TypeData,
    /**
     * Unique ID of the merged entry.
     * But because of PhotoPrism pagination strategy, it may be duplicated on adjacent pages.
     */
    val uid: String,
    /**
     * Original graphics width.
     */
    val width: Int,
    /**
     * Original graphics height.
     */
    val height: Int,
    /**
     * **Local** time of taking (or at least indexing) the shot.
     * While the value is not actually in UTC (unless really taken there),
     * all the temporal operations (Calendar, DateFormat) must be done in UTC
     * to get proper result.
     *
     * *For example, if a photo is taken on July 4th at 23:18 in Montenegro,
     * its local time 2023-07-04T23:18:32Z must be displayed and treated as July 4th, 23:18
     * regardless of whether the gallery is viewed from Montenegro or USA.*
     */
    val takenAtLocal: LocalDate,
    /**
     * Human-friendly title (PhotoPrism "Title").
     */
    val title: String,
    /**
     * Direct URL to open this media in the web viewer.
     */
    val webViewUrl: String,
    /**
     * Whether the entry is liked (added to favorites) or not.
     */
    var isFavorite: Boolean,
    /**
     * Whether the entry is hidden (marked as private) or not.
     */
    var isPrivate: Boolean,
    val hash: String,
    files: List<File>,
    previewUrlFactory: MediaPreviewUrlFactory,
) : WithThumbnail by WithThumbnailFromUrlFactory(hash, previewUrlFactory) {
    /**
     * Files associated with this media.
     */
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
        takenAtLocal = LocalDate(localDate = parsePhotoPrismDate(source.takenAtLocal)!!),
        title = source.title,
        webViewUrl = webUrlFactory.getWebViewUrl(source.uid),
        isFavorite = source.favorite,
        isPrivate = source.private,
        files = source.files.map { photoPrismFile ->
            File(
                source = photoPrismFile,
                thumbnailUrlFactory = previewUrlFactory,
                downloadUrlFactory = downloadUrlFactory,
            )
        }.toMutableList(),
        hash = source.hash,
        previewUrlFactory = previewUrlFactory,
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

        object Unknown : TypeData(TypeName.UNKNOWN)

        class Image(
            hash: String,
            mediaPreviewUrlFactory: MediaPreviewUrlFactory,
        ) : TypeData(TypeName.IMAGE),
            ViewableAsImage by ViewableAsImageWithUrlFactory(hash, mediaPreviewUrlFactory)

        class Raw(
            hash: String,
            mediaPreviewUrlFactory: MediaPreviewUrlFactory,
        ) : TypeData(TypeName.RAW),
            ViewableAsImage by ViewableAsImageWithUrlFactory(hash, mediaPreviewUrlFactory)

        class Animated(
            override val videoPreviewUrl: String,
        ) : TypeData(TypeName.ANIMATED), ViewableAsVideo

        class Live(
            override val videoPreviewUrl: String,
            /**
             * Non-zero duration of the full video in milliseconds,
             * or null if it couldn't be determined.
             */
            val fullDurationMs: Long?,
            val kind: Kind,
            hash: String,
            mediaPreviewUrlFactory: MediaPreviewUrlFactory,
        ) : TypeData(TypeName.LIVE), ViewableAsVideo,
            ViewableAsImage by ViewableAsImageWithUrlFactory(hash, mediaPreviewUrlFactory) {
            init {
                require((fullDurationMs ?: 0L) != 0L) {
                    "The full duration must be either null or positive"
                }
            }

            enum class Kind {
                /**
                 * Just a short video treated by PhotoPrism as a live photo
                 * having the still image generated from the first frame.
                 */
                SHORT_VIDEO,

                /**
                 * Real live photo with with a high quality still image
                 * taken at the end of the video.
                 * This kind has the true live photo magic ✨
                 *
                 * [Samsung: Motion Photos](https://www.samsung.com/au/support/mobile-devices/motion-photos/)
                 */
                SAMSUNG,

                /**
                 * Real live photo with a high quality still image
                 * taken at the middle of the 3 second the video.
                 * This kind has the true live photo magic ✨
                 *
                 * [Apple: Live Photos](https://support.apple.com/en-gb/HT207310)
                 */
                APPLE,

                /**
                 * Google motion still photo with a high quality still image
                 * taken, however, not during the video but before it.
                 * To get the magical effect, it must be played with a motion still
                 * stabilization, which the gallery is not capable of doing.
                 *
                 * [Google: Motion Stills](https://blog.research.google/2018/03/behind-motion-photos-technology-in.html)
                 */
                GOOGLE,

                /**
                 * Unknown kind of live photo.
                 */
                OTHER,

                ;
            }
        }

        class Video(
            override val videoPreviewUrl: String,
        ) : TypeData(TypeName.VIDEO), ViewableAsVideo

        class Vector(
            hash: String,
            mediaPreviewUrlFactory: MediaPreviewUrlFactory,
        ) : TypeData(TypeName.VECTOR),
            ViewableAsImage by ViewableAsImageWithUrlFactory(hash, mediaPreviewUrlFactory)

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
                        hash = source.hash,
                        mediaPreviewUrlFactory = previewUrlFactory,
                    )

                    TypeName.RAW.value -> Raw(
                        hash = source.hash,
                        mediaPreviewUrlFactory = previewUrlFactory,
                    )

                    TypeName.ANIMATED.value -> Animated(
                        videoPreviewUrl = previewUrlFactory.getVideoPreviewUrl(source),
                    )

                    TypeName.LIVE.value -> Live(
                        videoPreviewUrl = previewUrlFactory.getVideoPreviewUrl(source),
                        // Find the duration among video files.
                        fullDurationMs = source.files
                            .find { it.duration != null && it.duration > 0 }
                            ?.duration
                            ?.let(TimeUnit.NANOSECONDS::toMillis),
                        hash = source.hash,
                        kind = when {
                            source.let {
                                val videoFile = it.videoFile
                                val mainFile = it.mainFile

                                // Short videos have primary image file
                                // generated from the video file,
                                // while real live photos have the preview generated
                                // from the image file (HEIC) or don't have it at all.
                                mainFile != null
                                        && videoFile != null
                                        && videoFile != mainFile
                                        && videoFile.codec != "heic"
                                        && mainFile.name.startsWith(videoFile.name)
                            } ->
                                Live.Kind.SHORT_VIDEO

                            source.cameraMake == "Samsung" ->
                                Live.Kind.SAMSUNG

                            source.cameraMake == "Apple" ->
                                Live.Kind.APPLE

                            source.cameraMake == "Google" ->
                                Live.Kind.GOOGLE

                            else ->
                                Live.Kind.OTHER
                        },
                        mediaPreviewUrlFactory = previewUrlFactory,
                    )

                    TypeName.VIDEO.value -> Video(
                        videoPreviewUrl = previewUrlFactory.getVideoPreviewUrl(source),
                    )

                    TypeName.VECTOR.value -> Vector(
                        hash = source.hash,
                        mediaPreviewUrlFactory = previewUrlFactory,
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
        /**
         * Filename with the full path.
         */
        val name: String,
        /**
         * Unique ID of the file.
         */
        val uid: String,
        /**
         * Unique ID of the file's parent [GalleryMedia] (PhotoPrism "PhotoUID").
         */
        val mediaUid: String,
        val mimeType: String,
        val sizeBytes: Long?,
        /**
         * Direct URL to the small square static thumbnail.
         */
        val smallThumbnailUrl: String,
        /**
         * Direct URL download this file.
         */
        val downloadUrl: String,
    ) : Parcelable {
        constructor(
            source: PhotoPrismMergedPhoto.File,
            thumbnailUrlFactory: MediaPreviewUrlFactory,
            downloadUrlFactory: MediaFileDownloadUrlFactory,
        ) : this(
            name = source.name,
            uid = source.uid,
            mediaUid = source.photoUid,
            mimeType = source.mime ?: "application/octet-stream",
            sizeBytes = source.size,
            smallThumbnailUrl = thumbnailUrlFactory.getThumbnail224Url(source.hash),
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
}
