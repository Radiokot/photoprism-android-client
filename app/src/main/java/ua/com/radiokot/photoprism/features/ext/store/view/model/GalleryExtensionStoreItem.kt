package ua.com.radiokot.photoprism.features.ext.store.view.model

import ua.com.radiokot.photoprism.features.ext.data.model.GalleryExtension
import java.math.BigDecimal

class GalleryExtensionStoreItem(
    val extension: GalleryExtension,
    val price: BigDecimal,
    /**
     * ISO-4217 3-letter code.
     */
    val currency: String,
    val pageUrl: String,
    val isAlreadyActivated: Boolean,
)
