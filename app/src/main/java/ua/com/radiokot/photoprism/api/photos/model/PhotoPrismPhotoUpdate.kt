package ua.com.radiokot.photoprism.api.photos.model

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty

@JsonInclude(JsonInclude.Include.NON_NULL)
class PhotoPrismPhotoUpdate(
    @JsonProperty("Favorite")
    val favorite: Boolean? = null,
    @JsonProperty("Private")
    val private: Boolean? = null,
)
