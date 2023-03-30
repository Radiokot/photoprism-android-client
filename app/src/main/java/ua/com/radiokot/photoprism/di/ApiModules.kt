package ua.com.radiokot.photoprism.di

import org.koin.core.module.Module
import org.koin.core.parameter.parametersOf
import org.koin.core.qualifier._q
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.jackson.JacksonConverterFactory
import ua.com.radiokot.photoprism.api.albums.service.PhotoPrismAlbumsService
import ua.com.radiokot.photoprism.api.config.service.PhotoPrismClientConfigService
import ua.com.radiokot.photoprism.api.photos.service.PhotoPrismPhotosService
import ua.com.radiokot.photoprism.api.session.service.PhotoPrismSessionService
import ua.com.radiokot.photoprism.api.util.SyncCallAdapter
import ua.com.radiokot.photoprism.env.data.model.EnvSession

private class EnvRetrofitParams(
    val apiUrl: String,
    val httpClient: HttpClient,
)

val retrofitApiModules: List<Module> = listOf(
    module {
        includes(envModules)

        factory<Retrofit>(named<EnvRetrofitParams>()) { (params: EnvRetrofitParams) ->
            Retrofit.Builder()
                .baseUrl(params.apiUrl)
                .addConverterFactory(JacksonConverterFactory.create(get()))
                .addCallAdapterFactory(SyncCallAdapter.Factory)
                .client(params.httpClient)
                .build()
        }

        factory<PhotoPrismSessionService> { (apiUrl: String) ->
            get<Retrofit>(named<EnvRetrofitParams>()) {
                parametersOf(
                    EnvRetrofitParams(
                        apiUrl = apiUrl,
                        httpClient = get()
                    )
                )
            }
                .create(PhotoPrismSessionService::class.java)
        }

        factory<PhotoPrismClientConfigService> { (apiUrl: String, sessionId: String) ->
            get<Retrofit>(named<EnvRetrofitParams>()) {
                parametersOf(
                    EnvRetrofitParams(
                        apiUrl = apiUrl,
                        httpClient = get(_q<EnvHttpClientParams>()) {
                            parametersOf(
                                EnvHttpClientParams(
                                    sessionAwareness = EnvHttpClientParams.SessionAwareness(
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
            scoped<Retrofit> {
                val session = get<EnvSession>()
                get(_q<EnvRetrofitParams>()) {
                    parametersOf(
                        EnvRetrofitParams(
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

            scoped<PhotoPrismAlbumsService> {
                get<Retrofit>()
                    .create(PhotoPrismAlbumsService::class.java)
            } bind PhotoPrismAlbumsService::class
        }
    }
)