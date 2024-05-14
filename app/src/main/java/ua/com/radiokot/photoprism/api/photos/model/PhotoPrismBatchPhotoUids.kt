package ua.com.radiokot.photoprism.api.photos.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty

class PhotoPrismBatchPhotoUids
@JsonCreator
constructor(
    @JsonProperty("photos")
    val photoUids: Collection<String>,
)
