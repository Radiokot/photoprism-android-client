package ua.com.radiokot.photoprism.api.session.model

import com.fasterxml.jackson.annotation.JsonProperty

class PhotoPrismSessionCredentials(
    @JsonProperty("username")
    val username: String,
    @JsonProperty("password")
    val password: String,
    @JsonProperty("passcode")
    val passcode: String?,
)
