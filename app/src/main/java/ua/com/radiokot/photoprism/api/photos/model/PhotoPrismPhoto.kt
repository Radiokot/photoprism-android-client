package ua.com.radiokot.photoprism.api.photos.model

import com.fasterxml.jackson.annotation.JsonProperty

data class PhotoPrismPhoto(
    @JsonProperty("Hash")
    val hash: String,
    @JsonProperty("Width")
    val width: Int,
    @JsonProperty("Height")
    val height: Int,
    @JsonProperty("TakenAt")
    val takenAt: String,
    @JsonProperty("Type")
    val type: String,
    @JsonProperty("Name")
    val name: String,
    @JsonProperty("Files")
    val files: List<File>,
) {
    class File(
        @JsonProperty("Hash")
        val hash: String,
        @JsonProperty("Name")
        val name: String,
        @JsonProperty("Mime")
        val mime: String,
        @JsonProperty("Size")
        val size: Long,
    )
}