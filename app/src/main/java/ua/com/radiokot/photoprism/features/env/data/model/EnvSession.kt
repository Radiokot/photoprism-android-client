package ua.com.radiokot.photoprism.features.env.data.model

data class EnvSession(
    val apiUrl: String,
    var id: String,
    val previewToken: String,
    val downloadToken: String,
)