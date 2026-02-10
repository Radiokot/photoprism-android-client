package ua.com.radiokot.photoprism.features.ext.key.activation.data.model

import ua.com.radiokot.license.OfflineLicenseKey
import ua.com.radiokot.photoprism.features.ext.data.model.GalleryExtension
import java.util.Date

data class ParsedKey(
    val subject: String,
    val extensions: Set<GalleryExtension>,
    val expiresAt: Date?,
    val encoded: String,
) {
    constructor(source: OfflineLicenseKey) : this(
        subject = source.subject,
        extensions = source.features
            .mapNotNullTo(mutableSetOf(), GalleryExtension.entries::getOrNull),
        expiresAt = source.expiresAt,
        encoded = source.encode(),
    )
}
