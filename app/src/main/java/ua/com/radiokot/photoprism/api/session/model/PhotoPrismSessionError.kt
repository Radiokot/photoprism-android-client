package ua.com.radiokot.photoprism.api.session.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty

class PhotoPrismSessionError
@JsonCreator
constructor(
    @JsonProperty("code")
    val code: Int?,
) {
    companion object {
        const val CODE_PASSCODE_REQUIRED = 32
        const val CODE_INVALID_PASSCODE = 33
    }
}

