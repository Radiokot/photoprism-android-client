package ua.com.radiokot.photoprism.features.gallery.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import ua.com.radiokot.photoprism.api.photos.model.PhotoPrismMergedPhoto
import ua.com.radiokot.photoprism.api.photos.model.PhotoPrismMergedPhoto.File
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
     * Whether the entry is liked (added to favorites) or not.
     */
    var isFavorite: Boolean,
    /**
     * Whether the entry is hidden (marked as private) or not.
     */
    var isPrivate: Boolean,
    val hash: String,
    files: List<File>,
) {

    /**
     * Files associated with this media.
     */
    var files: List<File> = files
        private set

    val originalFile: File
        // https://github.com/photoprism/photoprism/blob/d6e509678642a4f72cd28b3d33c2aa171621817f/frontend/src/model/photo.js#L575
        get() {
            if (files.size < 2) {
                return files.first()
            }

            val fileMatchingType: File? = when (media.typeName) {
                TypeName.ANIMATED ->
                    files.find { it.mediaType == TypeName.IMAGE && it.root == "/" }

                TypeName.LIVE ->
                    files.find { it.mediaType == TypeName.VIDEO || it.mediaType == TypeName.LIVE && it.root == "/" }

                TypeName.RAW,
                TypeName.VIDEO,
                TypeName.VECTOR ->
                    files.find { it.mediaType == this.media.typeName && it.root == "/" }

                else ->
                    null
            }

            return fileMatchingType
                ?: files.find { it.isSidecar != true && it.type == "jpg" && it.root == "/" }
                ?: mainFile
        }

    private val mainFile: File
        get() = files.mainFile

    val videoFile: File?
        get() = files.videoFile

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

        return uid == other.uid
    }

    override fun hashCode(): Int {
        return uid.hashCode()
    }

    override fun toString(): String {
        return "GalleryMedia(uid='$uid', media=$media)"
    }

    companion object {
        fun fromPhotoPrism(source: PhotoPrismMergedPhoto): GalleryMedia {
            val files = source.files.map(::File).toMutableList()
            val typeData = TypeData.fromPhotoPrism(
                source = source,
                files = files,
            )

            return GalleryMedia(
                media = typeData,
                uid = source.uid,
                width = source.width,
                height = source.height,
                takenAtLocal = LocalDate(localDate = parsePhotoPrismDate(source.takenAtLocal)!!),
                title = source.title,
                isFavorite = source.favorite,
                isPrivate = source.private,
                files = files,
                hash = source.hash,
            )
        }
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

        companion object {
            fun fromPhotoPrism(type: String?): TypeName = when (type) {
                IMAGE.value -> IMAGE
                RAW.value -> RAW
                ANIMATED.value -> ANIMATED
                LIVE.value -> LIVE
                VIDEO.value -> VIDEO
                VECTOR.value -> VECTOR
                SIDECAR.value -> SIDECAR
                TEXT.value -> TEXT
                OTHER.value -> OTHER
                else -> UNKNOWN
            }
        }
    }

    sealed class TypeData(val typeName: TypeName) {

        object Image : TypeData(TypeName.IMAGE), Viewable.AsImage

        object Raw : TypeData(TypeName.RAW), Viewable.AsImage

        object Vector : TypeData(TypeName.VECTOR), Viewable.AsImage

        object Animated : TypeData(TypeName.ANIMATED), Viewable.AsVideo

        object Video : TypeData(TypeName.VIDEO), Viewable.AsVideo

        class Live(
            /**
             * Non-zero duration of the full video in milliseconds,
             * or null if it couldn't be determined.
             */
            val fullDurationMs: Long?,
            val kind: Kind,
        ) : TypeData(TypeName.LIVE), Viewable.AsVideo, Viewable.AsImage {
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

        object Sidecar : TypeData(TypeName.SIDECAR)

        object Text : TypeData(TypeName.TEXT)

        object Other : TypeData(TypeName.OTHER)

        object Unknown : TypeData(TypeName.UNKNOWN)

        companion object {
            fun fromPhotoPrism(
                source: PhotoPrismMergedPhoto,
                files: List<File>,
            ): TypeData =
                when (val type = source.type) {
                    TypeName.UNKNOWN.value -> Unknown

                    TypeName.IMAGE.value -> Image

                    TypeName.RAW.value -> Raw

                    TypeName.ANIMATED.value -> Animated

                    TypeName.VIDEO.value -> Video

                    TypeName.VECTOR.value -> Vector

                    TypeName.SIDECAR.value -> Sidecar

                    TypeName.TEXT.value -> Text

                    TypeName.OTHER.value -> Other

                    TypeName.LIVE.value -> Live(
                        // Find the duration among video files.
                        fullDurationMs = files
                            .find { it.duration != null && it.duration > 0 }
                            ?.duration
                            ?.let(TimeUnit.NANOSECONDS::toMillis),
                        kind = when {
                            (files.mainFile to files.videoFile).let { (mainFile, videoFile) ->
                                // Short videos have primary image file
                                // generated from the video file,
                                // while real live photos have the preview generated
                                // from the image file (HEIC) or don't have it at all.
                                videoFile != null
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
                    )

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
        val mediaType: TypeName,
        val sizeBytes: Long?,
        val isPrimary: Boolean?,
        val isSidecar: Boolean?,
        val isVideo: Boolean?,
        val type: String?,
        val codec: String?,
        val duration: Long?,
        val frames: Long?,
        val root: String?,
        val hash: String,
    ) : Parcelable {

        constructor(
            source: PhotoPrismMergedPhoto.File,
        ) : this(
            name = source.name,
            uid = source.uid,
            mediaUid = source.photoUid,
            mimeType = source.mime ?: "application/octet-stream",
            mediaType = TypeName.fromPhotoPrism(source.mediaType),
            sizeBytes = source.size,
            isPrimary = source.primary,
            isSidecar = source.sidecar,
            isVideo = source.video,
            type = source.fileType,
            codec = source.codec,
            duration = source.duration,
            frames = source.frames,
            root = source.root,
            hash = source.hash,
        )

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as File

            return uid == other.uid
        }

        override fun hashCode(): Int {
            return uid.hashCode()
        }

        override fun toString(): String {
            return "File(uid='$uid', name='$name')"
        }
    }
}

private val Collection<GalleryMedia.File>.mainFile: GalleryMedia.File
    // https://github.com/photoprism/photoprism/blob/d6e509678642a4f72cd28b3d33c2aa171621817f/frontend/src/model/photo.js#L540
    get() {
        if (size < 2) {
            return first()
        }

        return find { it.isPrimary == true }
            ?: find { it.type == "jpg" || it.type == "png" }
            ?: find { it.isSidecar != true }
            ?: first()
    }

private val Collection<GalleryMedia.File>.videoFile: GalleryMedia.File?
    // https://github.com/photoprism/photoprism/blob/d6e509678642a4f72cd28b3d33c2aa171621817f/frontend/src/model/photo.js#L479
    get() {
        return find { it.codec == "avc1" }
            ?: find { it.type == "mp4" }
            ?: find { it.isVideo == true }
            ?: animatedFile
    }

private val Collection<GalleryMedia.File>.animatedFile: GalleryMedia.File?
    // https://github.com/photoprism/photoprism/blob/d6e509678642a4f72cd28b3d33c2aa171621817f/frontend/src/model/photo.js#L501
    get() {
        return find { it.type == "gif" || it.duration != null || it.frames != null }
    }
