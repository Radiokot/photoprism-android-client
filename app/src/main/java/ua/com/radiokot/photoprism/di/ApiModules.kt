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
import ua.com.radiokot.photoprism.features.env.data.model.EnvSession

private enum class InjectedRetrofit {
    WITH_PARAMS,
    ;
}

val retrofitApiModules: List<Module> = listOf(
    module {
        includes(httpModules)

        factory<Retrofit>(named(InjectedRetrofit.WITH_PARAMS)) { (apiUrl: String, sessionId: String?) ->
            Retrofit.Builder()
                .baseUrl(apiUrl)
                .addConverterFactory(JacksonConverterFactory.create(get()))
                .addCallAdapterFactory(SyncCallAdapter.Factory)
                .client(
                    if (sessionId != null)
                        get()
                    else
                        get(named(InjectedHttpClient.WITH_SESSION)) { parametersOf(sessionId) }
                )
                .build()
        }

        factory<PhotoPrismSessionService> { (apiUrl: String) ->
            get<Retrofit>(named(InjectedRetrofit.WITH_PARAMS)) { parametersOf(apiUrl, null) }
                .create(PhotoPrismSessionService::class.java)
        }

        factory<PhotoPrismClientConfigService> { (apiUrl: String, sessionId: String) ->
            get<Retrofit>(named(InjectedRetrofit.WITH_PARAMS)) { parametersOf(apiUrl, sessionId) }
                .create(PhotoPrismClientConfigService::class.java)
        }

        scope<EnvSession> {
            scoped<OkHttpClient> {
                val session = get<EnvSession>()
                get(named(InjectedHttpClient.WITH_SESSION)) { parametersOf(session.id) }
            }

            scoped<Retrofit> {
                val session = get<EnvSession>()
                get(named(InjectedRetrofit.WITH_PARAMS)) {
                    parametersOf(
                        session.apiUrl,
                        session.id
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