package ua.com.radiokot.photoprism.api.albums.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty

data class PhotoPrismAlbum
@JsonCreator
constructor(
    @JsonProperty("UID")
    val uid: String,
    @JsonProperty("Title")
    val title: String,
    @JsonProperty("Thumb")
    val thumb: String,
    @JsonProperty("Type")
    val type: String,
    @JsonProperty("CreatedAt")
    val createdAt: String,
    @JsonProperty("Favorite")
    val favorite: Boolean,
    @JsonProperty("Description")
    val description: String,
    @JsonProperty("Path")
    val path: String?,
    @JsonProperty("Year")
    val year: Int,
    @JsonProperty("Month")
    val month: Int,
    @JsonProperty("Day")
    val day: Int,
)
