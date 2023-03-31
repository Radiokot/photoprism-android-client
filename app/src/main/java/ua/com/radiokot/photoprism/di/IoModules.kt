package ua.com.radiokot.photoprism.di

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import mu.KotlinLogging
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.core.module.Module
import org.koin.dsl.bind
import org.koin.dsl.module
import java.util.concurrent.TimeUnit

typealias HttpClient = OkHttpClient
typealias JsonObjectMapper = ObjectMapper

val ioModules: List<Module> = listOf(
    // JSON
    module {
        single<ObjectMapper> {
            jacksonObjectMapper()
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        }.bind(JsonObjectMapper::class)
    },

    // HTTP
    module {
        single {
            val logger = KotlinLogging.logger("HTTP")
            HttpLoggingInterceptor(logger::info).apply {
                level =
                    if (logger.isDebugEnabled)
                        HttpLoggingInterceptor.Level.BODY
                    else
                        HttpLoggingInterceptor.Level.BASIC
            }
        }.bind(HttpLoggingInterceptor::class)

        factory {
            OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
        }.bind(OkHttpClient.Builder::class)

        single {
            get<OkHttpClient.Builder>()
                .addInterceptor(get<HttpLoggingInterceptor>())
                .build()
        }.bind(HttpClient::class)
    },
)