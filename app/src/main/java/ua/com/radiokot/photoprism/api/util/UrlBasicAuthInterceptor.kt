package ua.com.radiokot.photoprism.api.util

import okhttp3.Credentials
import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.Response
import ua.com.radiokot.photoprism.extension.withMaskedCredentials

/**
 * An [Interceptor] that takes [HttpUrl.username] and [HttpUrl.password]
 * and makes an "Authorization: Basic" header from them.
 *
 * To avoid logging sensitive data, once handled the URL credentials are masked.
 */
class UrlBasicAuthInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val url = request.url
        val urlUsername = url.username
        val urlPassword = url.password

        val requestToProceed =
            if ((urlUsername.isNotEmpty() || urlPassword.isNotEmpty())
                && request.header(AUTH_HEADER) == null
            )
                request.newBuilder()
                    .header(
                        AUTH_HEADER,
                        Credentials.basic(urlUsername, urlPassword, Charsets.UTF_8)
                    )
                    .url(url.withMaskedCredentials())
                    .build()
            else
                request

        return chain.proceed(requestToProceed)
    }

    private companion object {
        private const val AUTH_HEADER = "Authorization"
    }
}