package ua.com.radiokot.photoprism.api.util

import okhttp3.Credentials
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response

/**
 * An interceptor adding header of given [name] to the requests.
 *
 * @see Request.Builder.addHeader
 */
class HeaderInterceptor(
    val name: String,
    val lazyValue: () -> Any,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response =
        chain.request()
            .newBuilder()
            .addHeader(name, lazyValue().toString())
            .build()
            .let(chain::proceed)

    companion object {
        /**
         * @return an interceptor injecting User-Agent.
         */
        fun userAgent(
            name: String,
            version: String,
            extension: String?,
        ): HeaderInterceptor {
            val userAgent = "$name/$version" +
                    if (extension != null)
                        " $extension"
                    else
                        ""

            return HeaderInterceptor(
                name = "User-Agent",
                lazyValue = { userAgent },
            )
        }

        /**
         * @return an interceptor injecting Authorization.
         *
         * @see [Credentials.basic]
         */
        fun authorization(
            lazyValue: () -> Any,
        ) = HeaderInterceptor(
            name = "Authorization",
            lazyValue = lazyValue,
        )
    }
}
