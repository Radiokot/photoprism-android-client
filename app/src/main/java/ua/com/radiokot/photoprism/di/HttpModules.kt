package ua.com.radiokot.photoprism.di

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import mu.KotlinLogging
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.core.module.Module
import org.koin.core.parameter.parametersOf
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module
import ua.com.radiokot.photoprism.api.session.service.PhotoPrismSessionService
import ua.com.radiokot.photoprism.api.util.SessionAwarenessInterceptor
import ua.com.radiokot.photoprism.api.util.SessionRenewalInterceptor
import ua.com.radiokot.photoprism.features.env.data.model.EnvConnection
import java.util.concurrent.TimeUnit

class InjectedHttpClientParams(
    val sessionAwareness: SessionAwareness?,
) {
    class SessionAwareness(
        val sessionIdProvider: () -> String,
        val renewal: Renewal?,
    ) {
        class Renewal(
            val credentialsProvider: () -> EnvConnection.Auth.Credentials,
            val sessionService: PhotoPrismSessionService,
            val onSessionRenewed: ((newId: String) -> Unit)?,
        )
    }
}

val httpModules: List<Module> = listOf(
    // JSON
    module {
        single<ObjectMapper> {
            jacksonObjectMapper()
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        }
    },

    // HTTP
    module {
        fun getLoggingInterceptor(): HttpLoggingInterceptor {
            val logger = KotlinLogging.logger("HTTP")
            return HttpLoggingInterceptor(logger::info).apply {
                level =
                    if (logger.isDebugEnabled)
                        HttpLoggingInterceptor.Level.BODY
                    else
                        HttpLoggingInterceptor.Level.BASIC
            }
        }

        fun getDefaultBuilder(): OkHttpClient.Builder {
            return OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
        }

        factory(named<InjectedHttpClientParams>()) { (params: InjectedHttpClientParams) ->
            val builder = getDefaultBuilder()

            if (params.sessionAwareness != null) {
                if (params.sessionAwareness.renewal != null) {
                    builder.addInterceptor(
                        SessionRenewalInterceptor(
                            credentialsProvider = params.sessionAwareness.renewal.credentialsProvider,
                            onSessionRenewed = params.sessionAwareness.renewal.onSessionRenewed,
                            sessionService = params.sessionAwareness.renewal.sessionService,
                        )
                    )
                }

                builder.addInterceptor(
                    SessionAwarenessInterceptor(
                        sessionIdProvider = params.sessionAwareness.sessionIdProvider,
                        sessionIdHeaderName = "X-Session-ID"
                    )
                )
            }

            builder
                .addInterceptor(getLoggingInterceptor())
                .build()
        }.bind(OkHttpClient::class)

        single {
            get<OkHttpClient>(named<InjectedHttpClientParams>()) {
                parametersOf(
                    InjectedHttpClientParams(
                        sessionAwareness = null,
                    )
                )
            }
        }.bind(OkHttpClient::class)
    },
)