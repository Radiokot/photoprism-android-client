package ua.com.radiokot.photoprism.api.albums.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import io.reactivex.rxjava3.core.Single

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
) {
    lateinit var cached: Single<Boolean>
}
