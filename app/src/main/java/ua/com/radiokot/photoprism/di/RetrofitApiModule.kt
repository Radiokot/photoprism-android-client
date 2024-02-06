package ua.com.radiokot.photoprism.di

import okhttp3.HttpUrl
import org.koin.core.qualifier._q
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.jackson.JacksonConverterFactory
import ua.com.radiokot.photoprism.api.albums.service.PhotoPrismAlbumsService
import ua.com.radiokot.photoprism.api.config.service.PhotoPrismClientConfigService
import ua.com.radiokot.photoprism.api.faces.service.PhotoPrismFacesService
import ua.com.radiokot.photoprism.api.photos.service.PhotoPrismPhotosService
import ua.com.radiokot.photoprism.api.session.service.PhotoPrismSessionService
import ua.com.radiokot.photoprism.api.subjects.service.PhotoPrismSubjectsService
import ua.com.radiokot.photoprism.api.util.SyncCallAdapter
import ua.com.radiokot.photoprism.env.data.model.EnvConnectionParams
import ua.com.radiokot.photoprism.env.data.model.EnvSession

private class EnvRetrofitParams(
    val apiUrl: HttpUrl,
    val httpClient: HttpClient,
) : SelfParameterHolder()

class EnvPhotoPrismSessionServiceParams(
    val envConnectionParams: EnvConnectionParams,
) : SelfParameterHolder()

class EnvPhotoPrismClientConfigServiceParams(
    val envConnectionParams: EnvConnectionParams,
    val sessionId: String,
) : SelfParameterHolder()

val retrofitApiModule = module {
    includes(envModule)

    factory<Retrofit>(_q<EnvRetrofitParams>()) { params ->
        params as EnvRetrofitParams
        Retrofit.Builder()
            .baseUrl(params.apiUrl)
            .addConverterFactory(JacksonConverterFactory.create(get()))
            .addCallAdapterFactory(SyncCallAdapter.Factory)
            .client(params.httpClient)
            .build()
    }

    factory<PhotoPrismSessionService>(_q<EnvPhotoPrismSessionServiceParams>()) { params ->
        params as EnvPhotoPrismSessionServiceParams
        get<Retrofit>(_q<EnvRetrofitParams>()) {
            EnvRetrofitParams(
                apiUrl = params.envConnectionParams.apiUrl,
                httpClient = get(_q<EnvHttpClientParams>()) {
                    EnvHttpClientParams(
                        sessionAwareness = null,
                        clientCertificateAlias = params.envConnectionParams.clientCertificateAlias,
                    )
                }
            )
        }
            .create(PhotoPrismSessionService::class.java)
    }

    factory<PhotoPrismClientConfigService>(_q<EnvPhotoPrismClientConfigServiceParams>()) { params ->
        params as EnvPhotoPrismClientConfigServiceParams
        get<Retrofit>(_q<EnvRetrofitParams>()) {
            EnvRetrofitParams(
                apiUrl = params.envConnectionParams.apiUrl,
                httpClient = get(_q<EnvHttpClientParams>()) {
                    EnvHttpClientParams(
                        sessionAwareness = EnvHttpClientParams.SessionAwareness(
                            sessionIdProvider = { params.sessionId },
                            renewal = null,
                        ),
                        clientCertificateAlias = params.envConnectionParams.clientCertificateAlias,
                    )
                }
            )
        }
            .create(PhotoPrismClientConfigService::class.java)
    }

    scope<EnvSession> {
        scoped<Retrofit> {
            val session = get<EnvSession>()
            get(_q<EnvRetrofitParams>()) {
                EnvRetrofitParams(
                    apiUrl = session.envConnectionParams.apiUrl,
                    httpClient = get()
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
        }

        scoped<PhotoPrismSubjectsService> {
            get<Retrofit>()
                .create(PhotoPrismSubjectsService::class.java)
        }

        scoped<PhotoPrismFacesService> {
            get<Retrofit>()
                .create(PhotoPrismFacesService::class.java)
        }
    }
}
