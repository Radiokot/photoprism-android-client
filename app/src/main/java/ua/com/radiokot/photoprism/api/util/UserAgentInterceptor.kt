package ua.com.radiokot.photoprism.api.util

import okhttp3.Interceptor
import okhttp3.Response

class UserAgentInterceptor(
    name: String,
    version: String,
    extension: String?,
) : Interceptor {
    val userAgent = "$name/$version" +
            if (extension != null)
                " $extension"
            else
                ""

    override fun intercept(chain: Interceptor.Chain): Response {
        return chain.request()
            .newBuilder()
            .header("User-Agent", userAgent)
            .build()
            .let(chain::proceed)
    }
}
