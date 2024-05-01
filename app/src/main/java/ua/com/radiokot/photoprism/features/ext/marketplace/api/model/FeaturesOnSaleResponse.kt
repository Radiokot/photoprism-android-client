package ua.com.radiokot.photoprism.features.ext.marketplace.api.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty

class FeaturesOnSaleResponse
@JsonCreator
constructor(
    @JsonProperty("data")
    val data: List<FeatureOnSale>,
) {
    class FeatureOnSale
    @JsonCreator
    constructor(
        @JsonProperty("id")
        val id: String,
        @JsonProperty("attributes")
        val attributes: Attributes,
    ) {
        class Attributes
        @JsonCreator
        constructor(
            @JsonProperty("price")
            val price: String,
        )
    }
}
