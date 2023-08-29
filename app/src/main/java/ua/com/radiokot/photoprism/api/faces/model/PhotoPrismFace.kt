package ua.com.radiokot.photoprism.api.faces.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty

data class PhotoPrismFace
@JsonCreator
constructor(
    @JsonProperty("ID")
    val id: String,
    @JsonProperty("Thumb")
    val thumb: String,
    @JsonProperty("Name")
    val name: String,
    @JsonProperty("Samples")
    val samples: Int,
)
