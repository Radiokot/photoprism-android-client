package ua.com.radiokot.photoprism.api.util

import okhttp3.Interceptor
import okhttp3.Response
import ua.com.radiokot.photoprism.env.data.model.EnvAuth
import ua.com.radiokot.photoprism.env.data.model.EnvSession
import ua.com.radiokot.photoprism.env.data.model.SessionExpiredException
import ua.com.radiokot.photoprism.env.logic.SessionCreator
import ua.com.radiokot.photoprism.extension.kLogger
import java.util.concurrent.atomic.AtomicBoolean

class SynchronizedSessionRenewalInterceptor(
    private val sessionCreator: SessionCreator,
    private val authProvider: () -> EnvAuth,
    private val onSessionRenewed: ((newSession: EnvSession) -> Unit)?,
) : Interceptor {
    private val log = kLogger("SyncSessionRenewalInterceptor")

    private val isRenewingSession = AtomicBoolean(false)
    private var renewedSession: EnvSession? = null
    private var sessionRenewalError: Throwable? = null

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()

        try {
            return chain.proceed(request)
        } catch (e: SessionExpiredException) {
            log.debug {
                "intercept(): got_session_expired_exception:" +
                        "\nmessage=${e.message}"
            }

            // Do not retry if already retrying.
            if (request.header(RETRY_HEADER) == null) {
                val newSession = getRenewedSessionSync(
                    authProvider = authProvider,
                    onSessionRenewed = onSessionRenewed,
                )

                log.debug {
                    "intercept(): got_new_session:" +
                            "\nthread=${Thread.currentThread()}" +
                            "\nid=${newSession.id}"
                }

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

    /**
     * Renews the session while ensuring not to run multiple renewal simultaneously.
     * If there is a running renewal, its result will be returned instead of running a new one.
     *
     * @return the renewed session
     */
    private fun getRenewedSessionSync(
        authProvider: () -> EnvAuth,
        onSessionRenewed: ((newSession: EnvSession) -> Unit)?,
    ): EnvSession =
        if (isRenewingSession.compareAndSet(false, true)) {
            // If no session renewal is running, do it in this thread.
            synchronized(isRenewingSession) {
                log.debug {
                    "getRenewedSession(): creating_new_session:" +
                            "\nthread=${Thread.currentThread()}"
                }

                renewedSession = null
                sessionRenewalError = null

                try {
                    renewedSession = sessionCreator.createSession(
                        auth = authProvider.invoke(),
                    )

                    log.debug {
                        "getRenewedSession(): new_session_created:" +
                                "\nid=${renewedSession?.id}"
                    }
                } catch (e: Throwable) {
                    sessionRenewalError = e

                    log.error(e) {
                        "getRenewedSession(): session_creation_failed"
                    }
                }

                isRenewingSession.set(false)

                // Only invoke the callback if renewed successfully.
                if (onSessionRenewed != null) {
                    renewedSession?.let(onSessionRenewed)
                }

                return@synchronized renewedSession
                    ?: throw checkNotNull(sessionRenewalError) {
                        "Failed to renew the session, but there is no error"
                    }
            }
        } else {
            log.debug {
                "getRenewedSession(): waiting_for_ongoing_renewal:" +
                        "\nthread=${Thread.currentThread()}"
            }

            // If there is a running renewal, just wait for it to end and return the result.
            synchronized(isRenewingSession) {
                return@synchronized renewedSession
                    ?: throw checkNotNull(sessionRenewalError) {
                        "Failed to renew the session, but there is no error"
                    }
            }
        }

    private companion object {
        private const val RETRY_HEADER = "X-Session-Renewal-Retry"
    }
}
