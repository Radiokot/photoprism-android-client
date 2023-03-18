package ua.com.radiokot.photoprism.features.env.data.model

import okhttp3.HttpUrl.Companion.toHttpUrl

data class EnvConnection(
    val apiUrl: String,
    val auth: Auth
) {
    sealed interface Auth {
        object Public : Auth

        // Do not make this class 'data', otherwise
        // the credentials will end up in the logs.
        class Credentials(
            val username: String,
            val password: String,
        ) : Auth
    }

    companion object {
        fun rootUrlToApiUrl(rootUrl: String): String =
            rootUrl
                .toHttpUrl()
                .newBuilder()
                .addPathSegment("api")
                .addPathSegment("") // Force trailing slash.
                .build()
                .toString()
    }
}