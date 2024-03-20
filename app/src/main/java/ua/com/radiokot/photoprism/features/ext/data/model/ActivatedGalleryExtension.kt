package ua.com.radiokot.photoprism.features.ext.data.model

import java.util.Date

data class ActivatedGalleryExtension(
    val type: GalleryExtension,
    val key: String,
    val expiresAt: Date?,
) {
    val isExpired: Boolean
        get() = expiresAt != null && System.currentTimeMillis() > expiresAt.time
}
