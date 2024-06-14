package ua.com.radiokot.photoprism.di

import android.content.Context
import android.os.Environment
import android.util.Log
import android.webkit.CookieManager
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import okhttp3.CookieJar
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.android.ext.koin.androidApplication
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module
import org.slf4j.impl.HandroidLoggerAdapter
import ua.com.radiokot.photoprism.BuildConfig
import ua.com.radiokot.photoprism.api.util.HeaderInterceptor
import ua.com.radiokot.photoprism.util.WebViewCookieJar
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
            // Native logger to be used for HTTP until SLF4J adapter is fixed:
            // https://gitlab.com/mvysny/slf4j-handroid/-/issues/11
            val nativeHttpLogger = HttpLoggingInterceptor.Logger { message ->
                Log.i("${HandroidLoggerAdapter.APP_NAME}:HTTP", message)
            }
            HttpLoggingInterceptor(nativeHttpLogger).apply {
                level =
                    if (BuildConfig.DEBUG)
                        HttpLoggingInterceptor.Level.BODY
                    else
                        HttpLoggingInterceptor.Level.BASIC
            }
        } bind HttpLoggingInterceptor::class

        singleOf(CookieManager::getInstance)

        // Use WebView cookies to pass proxy auth,
        // like Authelia, Umbrel, Cloudflare, etc.
        singleOf(::WebViewCookieJar) bind CookieJar::class

        factory {
            OkHttpClient.Builder()
                // Connect timeout to cut off dead network.
                .connectTimeout(30, TimeUnit.SECONDS)
                // Read and write timeouts are big because PhotoPrism
                // may run really slow on low-power servers.
                .readTimeout(1, TimeUnit.MINUTES)
                .writeTimeout(1, TimeUnit.MINUTES)
                .addInterceptor(
                    HeaderInterceptor.userAgent(
                        name = "PhotoPrismAndroid",
                        version = BuildConfig.VERSION_NAME,
                        extension = okhttp3.internal.userAgent,
                    )
                )
                .cookieJar(get())
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
