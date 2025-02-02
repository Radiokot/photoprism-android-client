package ua.com.radiokot.photoprism.features.ext.api.model

import com.fasterxml.jackson.annotation.JsonProperty

class KeyRenewalRequest(
    @JsonProperty("data")
    val data: Data,
) {
    constructor(
        key: String,
        hardware: String,
    ) : this(
        data = Data(
            attributes = Data.Attributes(
                key = key,
                hardware = hardware,
            ),
        ),
    )

    class Data(
        @JsonProperty("attributes")
        val attributes: Attributes,
        @JsonProperty("id")
        val id: String = System.currentTimeMillis().toString(),
        @JsonProperty("type")
        val type: String = "renewal-requests",
    ) {
        class Attributes(
            @JsonProperty("key")
            val key: String,
            @JsonProperty("hardware")
            val hardware: String,
        )
    }
}
