package ua.com.radiokot.photoprism.features.envconnection.data.model

import okhttp3.HttpUrl.Companion.toHttpUrl
import ua.com.radiokot.photoprism.env.data.model.EnvAuth

data class EnvConnection(
    val apiUrl: String,
    val auth: EnvAuth
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