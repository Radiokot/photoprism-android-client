package ua.com.radiokot.photoprism.env.data.model

data class EnvSession(
    var id: String,
    val envConnectionParams: EnvConnectionParams,
    val previewToken: String,
    val downloadToken: String,
)