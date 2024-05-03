package ua.com.radiokot.photoprism.features.ext.marketplace.view.model

import ua.com.radiokot.photoprism.features.ext.data.model.GalleryExtension
import java.math.BigDecimal

class GalleryExtensionMarketplaceItem(
    val extension: GalleryExtension,
    val price: BigDecimal,
    /**
     * ISO-4217 3-letter code.
     */
    val currency: String,
    val isAlreadyActivated: Boolean,
)
