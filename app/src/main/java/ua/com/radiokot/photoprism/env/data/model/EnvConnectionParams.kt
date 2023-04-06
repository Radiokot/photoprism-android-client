package ua.com.radiokot.photoprism.env.data.model

import okhttp3.HttpUrl.Companion.toHttpUrl

/**
 * Holds parameters required to connect to the environment.
 */
data class EnvConnectionParams(
    val apiUrl: String,
    val clientCertificateAlias: String?,
) {
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