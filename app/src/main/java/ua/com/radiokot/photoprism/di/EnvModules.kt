package ua.com.radiokot.photoprism.di

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.core.parameter.parametersOf
import org.koin.core.qualifier._q
import org.koin.dsl.bind
import org.koin.dsl.module
import ua.com.radiokot.photoprism.api.util.SessionAwarenessInterceptor
import ua.com.radiokot.photoprism.api.util.SessionRenewalInterceptor
import ua.com.radiokot.photoprism.base.data.storage.ObjectPersistence
import ua.com.radiokot.photoprism.env.data.model.EnvAuth
import ua.com.radiokot.photoprism.env.data.model.EnvSession
import ua.com.radiokot.photoprism.env.logic.PhotoPrismSessionCreator
import ua.com.radiokot.photoprism.env.logic.SessionCreator
import ua.com.radiokot.photoprism.extension.checkNotNull

class EnvHttpClientParams(
    val sessionAwareness: SessionAwareness?,
) {
    class SessionAwareness(
        val sessionIdProvider: () -> String,
        val renewal: Renewal?,
    ) {
        class Renewal(
            val authProvider: () -> EnvAuth,
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
                            authProvider = renewal.authProvider,
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
                val authPersistence = getOrNull<ObjectPersistence<EnvAuth>>(_q<EnvAuth>())
                val sessionPersistence = getOrNull<ObjectPersistence<EnvSession>>(_q<EnvSession>())

                val renewal: EnvHttpClientParams.SessionAwareness.Renewal? =
                    if (authPersistence != null && authPersistence.hasItem())
                        EnvHttpClientParams.SessionAwareness.Renewal(
                            authProvider = {
                                authPersistence.loadItem().checkNotNull {
                                    "There must be an auth data in order to renew the session"
                                }
                            },
                            sessionCreator = get { parametersOf(session.apiUrl) },
                            onSessionRenewed = {
                                session.id = it
                                sessionPersistence?.saveItem(session)
                            }
                        )
                    else
                        null

                get<HttpClient>(_q<EnvHttpClientParams>()) {
                    parametersOf(
                        EnvHttpClientParams(
                            sessionAwareness = EnvHttpClientParams.SessionAwareness(
                                sessionIdProvider = session::id,
                                renewal = renewal,
                            )
                        )
                    )
                }
            }.bind(HttpClient::class)
        }
    }
)