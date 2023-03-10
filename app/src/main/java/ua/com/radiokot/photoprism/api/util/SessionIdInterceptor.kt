package ua.com.radiokot.photoprism.api.util

import okhttp3.Interceptor
import okhttp3.Response

class SessionIdInterceptor(
    private val sessionId: String,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        return chain.proceed(
            chain.request().newBuilder()
                .addHeader("X-Session-ID", sessionId)
                .build()
        )
    }
}
