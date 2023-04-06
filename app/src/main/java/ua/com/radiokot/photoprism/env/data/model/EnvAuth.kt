package ua.com.radiokot.photoprism.env.data.model

sealed interface EnvAuth {
    object Public : EnvAuth

    // Do not make this class 'data', otherwise
    // the credentials will end up in the logs.
    class Credentials(
        val username: String,
        val password: String,
    ) : EnvAuth
}