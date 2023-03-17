package ua.com.radiokot.photoprism.di

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import mu.KotlinLogging
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module
import ua.com.radiokot.photoprism.api.util.SessionIdInterceptor
import java.util.concurrent.TimeUnit

enum class InjectedHttpClient {
    WITH_SESSION,
    ;
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

        factory(named(InjectedHttpClient.WITH_SESSION)) { (sessionId: String) ->
            getDefaultBuilder()
                .addInterceptor(SessionIdInterceptor(sessionId))
                .addInterceptor(getLoggingInterceptor())
                .build()
        }.bind(OkHttpClient::class)

        single {
            getDefaultBuilder()
                .addInterceptor(getLoggingInterceptor())
                .build()
        }.bind(OkHttpClient::class)
    },
)