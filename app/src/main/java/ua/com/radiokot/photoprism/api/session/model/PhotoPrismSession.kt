package ua.com.radiokot.photoprism.api.session.model

import com.fasterxml.jackson.annotation.JsonProperty

class PhotoPrismSession(
    @JsonProperty("id")
    val id: String,
)