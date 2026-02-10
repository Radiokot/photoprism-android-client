package ua.com.radiokot.photoprism.features.ext.store.data.model

import ua.com.radiokot.photoprism.extension.checkNotNull
import ua.com.radiokot.photoprism.features.ext.data.model.GalleryExtension
import ua.com.radiokot.photoprism.features.ext.api.model.FeaturesOnSaleResponse
import java.math.BigDecimal
import java.math.MathContext

class GalleryExtensionOnSale(
    val extension: GalleryExtension,
    val price: BigDecimal,
    val currency: String,
    val pageUrl: String,
) {
    constructor(featureOnSale: FeaturesOnSaleResponse.FeatureOnSale) : this(
        extension = featureOnSale.id.toInt().let { featureIndex ->
            GalleryExtension.entries.getOrNull(featureIndex).checkNotNull {
                "Extension for index '$featureIndex' not found"
            }
        },
        price = BigDecimal(featureOnSale.attributes.price, MathContext.DECIMAL32),
        currency = "USD",
        pageUrl = featureOnSale.attributes.page,
    )
}
