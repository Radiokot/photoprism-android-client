package ua.com.radiokot.photoprism.api.labels.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty

data class PhotoPrismLabel
@JsonCreator
constructor(
    @JsonProperty("UID")
    val uid: String,
    @JsonProperty("Name")
    val name: String,
    @JsonProperty("Thumb")
    val thumb: String,
    @JsonProperty("Favorite")
    val favorite: Boolean,
    @JsonProperty("PhotoCount")
    val photoCount: Int,
    @JsonProperty("CustomSlug")
    val customSlug: String,
)
