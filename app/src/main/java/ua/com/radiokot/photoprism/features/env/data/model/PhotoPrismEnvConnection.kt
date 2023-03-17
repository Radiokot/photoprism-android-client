package ua.com.radiokot.photoprism.features.env.data.model

data class PhotoPrismEnvConnection(
    val apiUrl: String,
    val auth: Auth
) {
    sealed interface Auth {
        object Public : Auth

        class Credentials(
            val login: String,
            val password: String,
        ) : Auth
    }
}