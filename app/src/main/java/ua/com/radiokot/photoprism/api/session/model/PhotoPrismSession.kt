package ua.com.radiokot.photoprism.api.session.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty

class PhotoPrismSession
@JsonCreator
constructor(
    @JsonProperty("id")
    val id: String,
)