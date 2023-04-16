package ua.com.radiokot.photoprism.api.util

import okhttp3.Interceptor
import okhttp3.Response
import ua.com.radiokot.photoprism.env.data.model.SessionExpiredException
import java.io.IOException
import java.net.HttpURLConnection

class SessionAwarenessInterceptor(
    private val sessionIdProvider: () -> String,
    private val sessionIdHeaderName: String,
) : Interceptor {
    @kotlin.jvm.Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val sessionId = sessionIdProvider()

        val response = chain.proceed(
            chain.request().newBuilder()
                .header(sessionIdHeaderName, sessionId)
                .build()
        )

        if (sessionId.isNotEmpty() && response.code == HttpURLConnection.HTTP_UNAUTHORIZED) {
            throw SessionExpiredException(sessionId)
        }

        return response
    }
}
