package ua.com.radiokot.photoprism.api.session.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty

data class PhotoPrismUser
@JsonCreator
constructor(
    @JsonProperty("UID")
    val uid: String,
)
