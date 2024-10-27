package ua.com.radiokot.photoprism.api.photos.model

import com.fasterxml.jackson.annotation.JsonProperty

class PhotoPrismPhotoUpdate(
    @JsonProperty("Private")
    val private: Boolean? = null,
)
