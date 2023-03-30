package ua.com.radiokot.photoprism.api.albums.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty

data class PhotoPrismAlbum
@JsonCreator
constructor(
    @JsonProperty("UID")
    val uid: String,
    @JsonProperty("Year")
    val year: Int,
    @JsonProperty("Month")
    val month: Int,
)