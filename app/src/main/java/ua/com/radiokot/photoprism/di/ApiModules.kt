package ua.com.radiokot.photoprism.di

import okhttp3.OkHttpClient
import org.koin.core.module.Module
import org.koin.core.parameter.parametersOf
import org.koin.core.qualifier.named
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.jackson.JacksonConverterFactory
import ua.com.radiokot.photoprism.api.PhotoPrismSession
import ua.com.radiokot.photoprism.api.config.service.PhotoPrismClientConfigService
import ua.com.radiokot.photoprism.api.util.SyncCallAdapter

val retrofitApiModules: List<Module> = listOf(
    module {
        includes(httpModules)

        scope<PhotoPrismSession> {
            scoped<OkHttpClient> {
                val session = get<PhotoPrismSession>()
                get(named(InjectedHttpClient.WITH_SESSION)) { parametersOf(session.id) }
            }

            scoped<Retrofit> {
                Retrofit.Builder()
                    .baseUrl(getProperty<String>("apiUrl"))
                    .addConverterFactory(JacksonConverterFactory.create(get()))
                    .addCallAdapterFactory(SyncCallAdapter.Factory)
                    .client(get())
                    .build()
            }

            scoped<PhotoPrismClientConfigService> {
                get<Retrofit>()
                    .create(PhotoPrismClientConfigService::class.java)
            }
        }
    }
)