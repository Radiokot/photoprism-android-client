package ua.com.radiokot.photoprism.api.photos.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * [photo_results.go](https://github.com/photoprism/photoprism/blob/47e9b20929e44bd63d32cb50a393371d24bcaff8/internal/search/photos_results.go#L15)
 */
data class PhotoPrismMergedPhoto
@JsonCreator
constructor(
    @JsonProperty("UID")
    val uid: String,
    @JsonProperty("Hash")
    val hash: String,
    @JsonProperty("Width")
    val width: Int,
    @JsonProperty("Height")
    val height: Int,
    @JsonProperty("TakenAtLocal")
    val takenAtLocal: String,
    @JsonProperty("Type")
    val type: String,
    @JsonProperty("Title")
    val title: String,
    @JsonProperty("Files")
    val files: List<File>,
    @JsonProperty("CameraMake")
    val cameraMake: String?,
    @JsonProperty("Favorite")
    val favorite: Boolean,
    @JsonProperty("Quality")
    val quality: Int,
    @JsonProperty("Private")
    val private: Boolean,
    @JsonProperty("Lat")
    val lat: Double,
    @JsonProperty("Lng")
    val lng: Double,
) {
    /**
     * [file.go](https://github.com/photoprism/photoprism/blob/47e9b20929e44bd63d32cb50a393371d24bcaff8/internal/entity/file.go#L40)
     */
    class File(
        // TODO Clarify whether the hash is actually optional.
        @JsonProperty("Hash")
        val hash: String,
        @JsonProperty("UID")
        val uid: String,
        @JsonProperty("PhotoUID")
        val photoUid: String,
        @JsonProperty("Name")
        val name: String,
        @JsonProperty("Mime")
        val mime: String?,
        @JsonProperty("FileType")
        val fileType: String?,
        @JsonProperty("MediaType")
        val mediaType: String?,
        @JsonProperty("Size")
        val size: Long?,
        @JsonProperty("Duration")
        val duration: Long?,
        @JsonProperty("Frames")
        val frames: Long?,
        @JsonProperty("Primary")
        val primary: Boolean?,
        @JsonProperty("Root")
        val root: String?,
        @JsonProperty("Video")
        val video: Boolean?,
        @JsonProperty("Codec")
        val codec: String?,
        @JsonProperty("Sidecar")
        val sidecar: Boolean?,
        @JsonProperty("Markers")
        val markers: List<Marker>?,
    ) {

        /**
         * [marker.go](https://github.com/photoprism/photoprism/blob/9e95c7e71ccdc155862d7e86b0cb071872ef4469/internal/entity/marker.go#L27)
         */
        class Marker(
            @JsonProperty("FaceID")
            val faceId: String?,
            @JsonProperty("SubjUID")
            val subjectUid: String?,
        )
    }
}
