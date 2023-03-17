package ua.com.radiokot.photoprism.api.session.model

import com.fasterxml.jackson.annotation.JsonProperty

class PhotoPrismSessionCredentials(
    @get:JsonProperty("username")
    val username: String,
    @get:JsonProperty("password")
    val password: String,
)