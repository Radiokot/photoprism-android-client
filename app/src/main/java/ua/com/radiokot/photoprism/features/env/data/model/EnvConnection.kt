package ua.com.radiokot.photoprism.features.env.data.model

import okhttp3.HttpUrl.Companion.toHttpUrl

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

    companion object {
        fun fromRootUrl(
            rootUrl: String,
            auth: Auth,
        ) = EnvConnection(
            apiUrl = rootUrl
                .toHttpUrl()
                .newBuilder()
                .addPathSegment("api")
                .addPathSegment("") // Force trailing slash.
                .build()
                .toString(),
            auth = auth
        )
    }
}