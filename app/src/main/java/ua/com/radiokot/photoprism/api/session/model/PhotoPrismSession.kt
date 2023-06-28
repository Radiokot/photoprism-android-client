package ua.com.radiokot.photoprism.api.session.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import ua.com.radiokot.photoprism.api.config.model.PhotoPrismClientConfig

class PhotoPrismSession
@JsonCreator
constructor(
    @JsonProperty("id")
    val id: String,
    @JsonProperty("config")
    val config: PhotoPrismClientConfig,
)
