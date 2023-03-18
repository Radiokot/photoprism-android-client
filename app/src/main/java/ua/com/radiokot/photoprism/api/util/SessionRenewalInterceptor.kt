package ua.com.radiokot.photoprism.api.util

import okhttp3.Interceptor
import okhttp3.Response
import ua.com.radiokot.photoprism.api.session.model.PhotoPrismSessionCredentials
import ua.com.radiokot.photoprism.api.session.service.PhotoPrismSessionService
import ua.com.radiokot.photoprism.extension.kLogger
import ua.com.radiokot.photoprism.features.env.data.model.EnvConnection
import ua.com.radiokot.photoprism.features.env.data.model.SessionExpiredException

class SessionRenewalInterceptor(
    private val sessionService: PhotoPrismSessionService,
    private val credentialsProvider: () -> EnvConnection.Auth.Credentials,
    private val onSessionRenewed: ((newSessionId: String) -> Unit)?,
) : Interceptor {
    private val log = kLogger("SessionRenewalInterceptor")

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()

        try {
            return chain.proceed(request)
        } catch (e: SessionExpiredException) {
            log.debug {
                "intercept(): got_session_expired_exception:" +
                        "\nmessage=${e.message}"
            }

            if (request.header(RETRY_HEADER) == null) {
                log.debug { "intercept(): creating_new_session" }

                val credentials = credentialsProvider()
                val newSessionId = sessionService
                    .createSession(
                        PhotoPrismSessionCredentials(
                            username = credentials.username,
                            password = credentials.password,
                        )
                    )
                    .id

                log.debug {
                    "intercept(): new_session_created:" +
                            "\nid=${newSessionId.substring(0..(newSessionId.length.coerceAtMost(5)))}..."
                }

                onSessionRenewed?.invoke(newSessionId)

                log.debug { "intercept(): retrying_failed_request" }

                val retryRequest = request.newBuilder()
                    .header(RETRY_HEADER, "true")
                    .build()

                return chain.proceed(retryRequest)
            } else {
                log.error { "intercept(): throw_because_of_failed_retry" }
                throw e
            }
        }
    }

    private companion object {
        private const val RETRY_HEADER = "X-Session-Renewal-Retry"
    }
}