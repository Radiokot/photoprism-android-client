package ua.com.radiokot.photoprism.api.photos.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty

class PhotoPrismBatchPhotoEdit
@JsonCreator
constructor(
    @JsonProperty("photos")
    val photoUids: Collection<String>,
    @JsonProperty("values")
    val values: Map<String, Any>
) {
    constructor(
        photoUids: Collection<String>,
        isFavorite: Boolean? = null,
        isPrivate: Boolean? = null,
    ) : this(
        photoUids = photoUids,
        values =
            arrayOf(
                "Favorite" to isFavorite,
                "Private" to isPrivate,
            )
                .filter { (_, value) -> value != null }
                .associate { (key, value) ->
                    key to mapOf(
                        "action" to "update",
                        "mixed" to false,
                        "value" to value,
                    )
                }
    )
}

