package ua.com.radiokot.photoprism.features.env.data.model

data class EnvConnection(
    val apiUrl: String,
    val auth: Auth
) {
    sealed interface Auth {
        object Public : Auth

        class Credentials(
            val username: String,
            val password: String,
        ) : Auth
    }
}