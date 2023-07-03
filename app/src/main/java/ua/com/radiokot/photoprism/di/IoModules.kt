package ua.com.radiokot.photoprism.di

import android.content.Context
import android.os.Environment
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import mu.KotlinLogging
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.android.ext.koin.androidApplication
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module
import ua.com.radiokot.photoprism.api.util.UrlBasicAuthInterceptor
import java.io.File
import java.util.concurrent.TimeUnit

typealias HttpClient = OkHttpClient
typealias JsonObjectMapper = ObjectMapper

const val EXTERNAL_DOWNLOADS_DIRECTORY = "external-downloads"
const val INTERNAL_DOWNLOADS_DIRECTORY = "internal-downloads"
const val INTERNAL_EXPORT_DIRECTORY = "internal-export"
const val VIDEO_CACHE_DIRECTORY = "video-cache"
const val IMAGE_CACHE_DIRECTORY = "image-cache"
const val APP_NO_BACKUP_PREFERENCES = "app-no-backup-preferences"

val ioModules: List<Module> = listOf(
    // JSON
    module {
        single<ObjectMapper> {
            jacksonObjectMapper()
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        } bind JsonObjectMapper::class
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
        } bind HttpLoggingInterceptor::class

        factory {
            OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .addInterceptor(UrlBasicAuthInterceptor())
        } bind OkHttpClient.Builder::class

        single {
            get<OkHttpClient.Builder>()
                .addInterceptor(get<HttpLoggingInterceptor>())
                .build()
        } bind HttpClient::class
    },

    // Directories
    module {
        fun File.createIfNotExists() = apply(File::mkdirs)

        single(named(EXTERNAL_DOWNLOADS_DIRECTORY)) {
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                .createIfNotExists()
        } bind File::class

        single(named(INTERNAL_DOWNLOADS_DIRECTORY)) {
            // See R.xml.file_provider_paths.
            File(androidApplication().filesDir.absolutePath + "/downloads")
                .createIfNotExists()
        } bind File::class

        single(named(INTERNAL_EXPORT_DIRECTORY)) {
            // See R.xml.file_provider_paths.
            File(androidApplication().filesDir.absolutePath + "/export")
                .createIfNotExists()
        } bind File::class

        single(named(VIDEO_CACHE_DIRECTORY)) {
            File(androidApplication().cacheDir, "video-cache")
                .createIfNotExists()
        } bind File::class

        single(named(IMAGE_CACHE_DIRECTORY)) {
            // Directory name matches Picasso default cache for backward compatibility.
            File(androidApplication().cacheDir, "picasso-cache")
                .createIfNotExists()
        } bind File::class
    },

    // Preferences.
    module {
        single(named(APP_NO_BACKUP_PREFERENCES)) {
            androidApplication().getSharedPreferences(
                "app",
                Context.MODE_PRIVATE,
            )
        }
    }
)
