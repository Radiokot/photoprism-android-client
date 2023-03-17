package ua.com.radiokot.photoprism.features.env.data.model

data class PhotoPrismSession(
    val apiUrl: String,
    val id: String,
    val previewToken: String,
    val downloadToken: String,
)