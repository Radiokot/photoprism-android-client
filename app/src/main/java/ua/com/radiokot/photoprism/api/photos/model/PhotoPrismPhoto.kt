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
)