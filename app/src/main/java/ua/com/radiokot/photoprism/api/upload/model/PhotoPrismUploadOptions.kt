package ua.com.radiokot.photoprism.api.upload.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty

data class PhotoPrismUploadOptions
@JsonCreator
constructor(
    @JsonProperty("albums")
    val albums: List<String>,
)
