package ua.com.radiokot.photoprism.features.ext.api.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty

class KeyRenewalResponse
@JsonCreator
constructor(
    @JsonProperty("data")
    val data: Data,
) {
    class Data
    @JsonCreator
    constructor(
        @JsonProperty("attributes")
        val attributes: Attributes,
    ) {
        class Attributes
        @JsonCreator
        constructor(
            @JsonProperty("key")
            val key: String,
        )
    }
}
