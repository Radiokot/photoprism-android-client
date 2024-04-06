package ua.com.radiokot.photoprism.api.session.model

import com.fasterxml.jackson.annotation.JsonProperty

// https://github.com/photoprism/photoprism/blob/cdd435f97c314782e8b8dc3ac31b6dadb9a74156/internal/form/login.go
class PhotoPrismSessionCredentials(
    @JsonProperty("username")
    val username: String,
    @JsonProperty("password")
    val password: String,
    @JsonProperty("code")
    val code: String?,
)
