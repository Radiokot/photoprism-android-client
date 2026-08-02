package ua.com.radiokot.photoprism.api.util

import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.internal.closeQuietly
import ua.com.radiokot.photoprism.env.data.model.SessionExpiredException
import java.io.IOException
import java.net.HttpURLConnection

class SessionAwarenessInterceptor(
    private val sessionIdProvider: () -> String,
    private val onFreshTokensReceived: ((
        previewToken: String?,
        downloadToken: String?,
    ) -> Unit)?,
) : Interceptor {
    @kotlin.jvm.Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val sessionId = sessionIdProvider()

        val response = chain.proceed(
            chain.request().newBuilder()
                .header("X-Session-ID", sessionId)
                .build()
        )

        if (onFreshTokensReceived != null) {
            val freshPreviewToken = response.header("X-Preview-Token")
            val freshDownloadToken = response.header("X-Download-Token")

            if (freshPreviewToken != null || freshDownloadToken != null) {
                onFreshTokensReceived(
                    freshPreviewToken,
                    freshDownloadToken,
                )
            }
        }

        if (sessionId.isNotEmpty() && response.code == HttpURLConnection.HTTP_UNAUTHORIZED) {
            response.closeQuietly()
            throw SessionExpiredException(sessionId)
        }

        return response
    }
}
