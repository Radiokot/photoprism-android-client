package ua.com.radiokot.photoprism.di

import android.content.Context
import android.os.Environment
import android.webkit.CookieManager
import androidx.work.WorkManager
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import mu.KotlinLogging
import okhttp3.CookieJar
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.android.ext.koin.androidApplication
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module
import ua.com.radiokot.photoprism.BuildConfig
import ua.com.radiokot.photoprism.api.util.HeaderInterceptor
import ua.com.radiokot.photoprism.util.LocalDate
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
                .registerModule(SimpleModule("LocalDateModule").apply {
                    addSerializer(LocalDate::class.java, object : JsonSerializer<LocalDate>() {
                        override fun serialize(
                            value: LocalDate,
                            gen: JsonGenerator,
                            serializers: SerializerProvider?
                        ) = gen.writeNumber(value.time)
                    })
                    addDeserializer(LocalDate::class.java, object : JsonDeserializer<LocalDate>() {
                        override fun deserialize(
                            p: JsonParser,
                            ctxt: DeserializationContext?
                        ) = LocalDate(p.longValue)
                    })
                })
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        } bind JsonObjectMapper::class
    },

    // HTTP
    module {
        single {
            val logger = KotlinLogging.logger("HTTP")
            HttpLoggingInterceptor(logger::info).apply {
                if (BuildConfig.DEBUG) {
                    level = HttpLoggingInterceptor.Level.BODY

                    // Do not log multipart forms.
                    // Logging them may cause OOM if there are big files.
                    skipBody(MultipartBody.FORM)
                } else {
                    level = HttpLoggingInterceptor.Level.BASIC
                }
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
    },

    // Work.
    module {
        single {
            WorkManager.getInstance(androidApplication())
        } bind WorkManager::class
    },
)
