package ua.com.radiokot.photoprism.extension

import okhttp3.HttpUrl

/**
 * @return copy of the URL with username and password replaced with [placeholder], if present,
 * or the same instance otherwise.
 */
fun HttpUrl.withMaskedCredentials(placeholder: String = "xxx"): HttpUrl =
    if (username.isNotEmpty() || password.isNotEmpty())
        newBuilder().username(placeholder).password(placeholder).build()
    else
        this