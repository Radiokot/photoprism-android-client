package ua.com.radiokot.photoprism.di

import okhttp3.OkHttpClient
import org.koin.core.module.Module
import org.koin.core.parameter.parametersOf
import org.koin.core.qualifier.named
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.jackson.JacksonConverterFactory
import ua.com.radiokot.photoprism.api.config.service.PhotoPrismClientConfigService
import ua.com.radiokot.photoprism.api.photos.service.PhotoPrismPhotosService
import ua.com.radiokot.photoprism.api.session.service.PhotoPrismSessionService
import ua.com.radiokot.photoprism.api.util.SyncCallAdapter
import ua.com.radiokot.photoprism.features.env.data.model.EnvConnection
import ua.com.radiokot.photoprism.features.env.data.model.EnvSession

private class InjectedRetrofitParams(
    val apiUrl: String,
    val httpClient: OkHttpClient,
)

private const val SESSION_UNAWARE_RETROFIT = "session-unaware-retrofit"
private const val SESSION_UNAWARE_HTTP_CLIENT = "session-unaware-http-client"

val retrofitApiModules: List<Module> = listOf(
    module {
        includes(httpModules)

        factory<Retrofit>(named<InjectedRetrofitParams>()) { (params: InjectedRetrofitParams) ->
            Retrofit.Builder()
                .baseUrl(params.apiUrl)
                .addConverterFactory(JacksonConverterFactory.create(get()))
                .addCallAdapterFactory(SyncCallAdapter.Factory)
                .client(params.httpClient)
                .build()
        }

        factory<PhotoPrismSessionService> { (apiUrl: String) ->
            get<Retrofit>(named<InjectedRetrofitParams>()) {
                parametersOf(
                    InjectedRetrofitParams(
                        apiUrl = apiUrl,
                        httpClient = get()
                    )
                )
            }
                .create(PhotoPrismSessionService::class.java)
        }

        factory<PhotoPrismClientConfigService> { (apiUrl: String, sessionId: String) ->
            get<Retrofit>(named<InjectedRetrofitParams>()) {
                parametersOf(
                    InjectedRetrofitParams(
                        apiUrl = apiUrl,
                        httpClient = get(named<InjectedHttpClientParams>()) {
                            parametersOf(
                                InjectedHttpClientParams(
                                    sessionAwareness = InjectedHttpClientParams.SessionAwareness(
                                        sessionIdProvider = { sessionId },
                                        renewal = null,
                                    )
                                )
                            )
                        }
                    )
                )
            }
                .create(PhotoPrismClientConfigService::class.java)
        }

        scope<EnvSession> {
            scoped<OkHttpClient> {
                val session = get<EnvSession>()
                get(named<InjectedHttpClientParams>()) {
                    parametersOf(
                        InjectedHttpClientParams(
                            sessionAwareness = InjectedHttpClientParams.SessionAwareness(
                                sessionIdProvider = { session.id },
                                renewal = InjectedHttpClientParams.SessionAwareness.Renewal(
                                    credentialsProvider = {
                                        // TODO: Get from persistence
                                        EnvConnection.Auth.Credentials(
                                            username = "username",
                                            password = "password",
                                        )
                                    },
                                    sessionService = get { parametersOf(session.apiUrl) },
                                    onSessionRenewed = {
                                        // TODO: Save to persistence
                                        session.id = it
                                    }
                                ),
                            )
                        )
                    )
                }
            }

            scoped<OkHttpClient>(named(SESSION_UNAWARE_HTTP_CLIENT)) {
                get(named<InjectedHttpClientParams>()) {
                    parametersOf(
                        InjectedHttpClientParams(
                            sessionAwareness = null,
                        )
                    )
                }
            }

            scoped<Retrofit> {
                val session = get<EnvSession>()
                get(named<InjectedRetrofitParams>()) {
                    parametersOf(
                        InjectedRetrofitParams(
                            apiUrl = session.apiUrl,
                            httpClient = get()
                        )
                    )
                }
            }

            scoped<PhotoPrismPhotosService> {
                get<Retrofit>()
                    .create(PhotoPrismPhotosService::class.java)
            }
        }
    }
)