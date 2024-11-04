package ua.com.radiokot.photoprism.api.albums.model

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty

@JsonInclude(JsonInclude.Include.NON_NULL)
class PhotoPrismAlbumCreation(
    @JsonProperty("Title")
    val title: String,
)
