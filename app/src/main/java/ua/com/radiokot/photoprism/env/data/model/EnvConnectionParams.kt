package ua.com.radiokot.photoprism.env.data.model

import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl

/**
 * Holds parameters required to connect to the environment.
 */
data class EnvConnectionParams(
    /**
     * Instance root URL.
     *
     * **May contain HTTP basic auth credentials**
     *
     * [What is Library root URL](https://github.com/Radiokot/photoprism-android-client/wiki/What-is-Library-root-URL%3F)
     */
    val rootUrl: HttpUrl,

    /**
     * Alias (name) of an installed client certificate to use for mTLS.
     */
    val clientCertificateAlias: String?,
) {
    constructor(
        rootUrlString: String,
        clientCertificateAlias: String?
    ) : this(
        rootUrl = rootUrlString.toHttpUrl(),
        clientCertificateAlias = clientCertificateAlias,
    )

    /**
     * API root URL (not including the version).
     *
     * **May contain HTTP basic auth credentials**
     */
    val apiUrl: HttpUrl
        get() = rootUrl
            .newBuilder()
            .addPathSegment("api")
            .addPathSegment("") // Force trailing slash.
            .build()

    /**
     * Library web client root URL.
     *
     * **May contain HTTP basic auth credentials**
     */
    val webLibraryUrl: HttpUrl
        get() = rootUrl
            .newBuilder()
            .addPathSegment("library")
            .addPathSegment("") // Force trailing slash.
            .build()
}