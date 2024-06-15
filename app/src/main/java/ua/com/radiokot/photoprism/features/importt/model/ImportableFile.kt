package ua.com.radiokot.photoprism.features.importt.model

import android.net.Uri

data class ImportableFile(
    val contentUri: Uri,
    val displayName: String,
    val mimeType: String?,
    val size: Long,
)
