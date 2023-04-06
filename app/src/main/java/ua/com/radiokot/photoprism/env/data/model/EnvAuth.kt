package ua.com.radiokot.photoprism.env.data.model

class EnvAuth(
    val credentials: Credentials?,
    val clientCertificateAlias: String?,
) {
    // Do not make this class 'data', otherwise
    // the credentials will end up in the logs.
    class Credentials(
        val username: String,
        val password: String,
    )
}