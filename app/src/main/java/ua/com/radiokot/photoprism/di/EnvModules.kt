package ua.com.radiokot.photoprism.di

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.core.parameter.parametersOf
import org.koin.core.qualifier._q
import org.koin.dsl.bind
import org.koin.dsl.module
import ua.com.radiokot.photoprism.api.util.SessionAwarenessInterceptor
import ua.com.radiokot.photoprism.api.util.SessionRenewalInterceptor
import ua.com.radiokot.photoprism.env.data.model.EnvAuth
import ua.com.radiokot.photoprism.env.data.model.EnvSession
import ua.com.radiokot.photoprism.env.logic.PhotoPrismSessionCreator
import ua.com.radiokot.photoprism.env.logic.SessionCreator

class EnvHttpClientParams(
    val sessionAwareness: SessionAwareness?,
) {
    class SessionAwareness(
        val sessionIdProvider: () -> String,
        val renewal: Renewal?,
    ) {
        class Renewal(
            val credentialsProvider: () -> EnvAuth.Credentials,
            val sessionCreator: SessionCreator,
            val onSessionRenewed: ((newId: String) -> Unit)?,
        )
    }
}

val envModules = listOf(
    module {
        includes(ioModules)

        // The factory must have a qualifier,
        // otherwise it is overridden by the scoped.
        factory(_q<EnvHttpClientParams>()) { (envParams: EnvHttpClientParams) ->
            val builder = get<OkHttpClient.Builder>()

            if (envParams.sessionAwareness != null) {
                val sessionAwareness = envParams.sessionAwareness

                if (sessionAwareness.renewal != null) {
                    val renewal = sessionAwareness.renewal

                    builder.addInterceptor(
                        SessionRenewalInterceptor(
                            credentialsProvider = renewal.credentialsProvider,
                            onSessionRenewed = renewal.onSessionRenewed,
                            sessionCreator = renewal.sessionCreator,
                        )
                    )
                }

                builder.addInterceptor(
                    SessionAwarenessInterceptor(
                        sessionIdProvider = sessionAwareness.sessionIdProvider,
                        sessionIdHeaderName = "X-Session-ID"
                    )
                )
            }

            builder
                .addInterceptor(get<HttpLoggingInterceptor>())
                .build()
        }.bind(HttpClient::class)

        factory { (apiUrl: String) ->
            PhotoPrismSessionCreator(
                sessionService = get { parametersOf(apiUrl) },
            )
        }.bind(SessionCreator::class)

        scope<EnvSession> {
            scoped {
                val session = get<EnvSession>()

                get<HttpClient>(_q<EnvHttpClientParams>()) {
                    parametersOf(
                        EnvHttpClientParams(
                            sessionAwareness = EnvHttpClientParams.SessionAwareness(
                                sessionIdProvider = { session.id },
                                renewal = EnvHttpClientParams.SessionAwareness.Renewal(
                                    credentialsProvider = {
                                        // TODO: Get from persistence
                                        EnvAuth.Credentials(
                                            username = "username",
                                            password = "password",
                                        )
                                    },
                                    sessionCreator = get { parametersOf(session.apiUrl) },
                                    onSessionRenewed = {
                                        // TODO: Save to persistence
                                        session.id = it
                                    }
                                ),
                            )
                        )
                    )
                }
            }.bind(HttpClient::class)
        }
    }
)