package ua.com.radiokot.photoprism

import android.app.Application
import android.os.Build
import io.reactivex.rxjava3.exceptions.UndeliverableException
import io.reactivex.rxjava3.plugins.RxJavaPlugins
import org.koin.android.ext.android.getKoin
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidFileProperties
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.qualifier.named
import org.slf4j.impl.HandroidLoggerAdapter
import ua.com.radiokot.photoprism.api.PhotoPrismSession
import ua.com.radiokot.photoprism.di.retrofitApiModules
import ua.com.radiokot.photoprism.extension.kLogger
import ua.com.radiokot.photoprism.features.gallery.di.galleryFeatureModules
import ua.com.radiokot.photoprism.features.viewer.di.mediaViewerFeatureModules
import java.io.IOException

class App : Application() {
    private val log = kLogger("App")

    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidLogger()
            androidContext(this@App)
            modules(
                retrofitApiModules
                        + galleryFeatureModules
                        + mediaViewerFeatureModules
            )
            properties(
                mapOf(
                    "sessionId" to BuildConfig.SESSION_ID,
                    "apiUrl" to BuildConfig.API_URL,
                )
            )
            androidFileProperties("app.properties")
        }

        getKoin().createScope(
            "session",
            named<PhotoPrismSession>(),
            PhotoPrismSession(
                id = getKoin().getProperty("sessionId")!!,
                previewToken = "3hjej82k",
                downloadToken = "bvmk6aml"
            )
        )

        initRxErrorHandler()
        initLogging()
    }

    private fun initRxErrorHandler() {
        RxJavaPlugins.setErrorHandler {
            var e: Throwable? = it
            if (e is UndeliverableException) {
                e = e.cause
            }
            if (e is IOException) {
                // fine, irrelevant network problem or API that throws on cancellation
                return@setErrorHandler
            }
            if (e is InterruptedException) {
                // fine, some blocking code was interrupted by a dispose call
                return@setErrorHandler
            }
            if ((e is NullPointerException) || (e is IllegalArgumentException)) {
                // that's likely a bug in the application
                Thread.currentThread().uncaughtExceptionHandler
                    ?.uncaughtException(Thread.currentThread(), e)
                return@setErrorHandler
            }
            if (e is IllegalStateException) {
                // that's a bug in RxJava or in a custom operator
                Thread.currentThread().uncaughtExceptionHandler
                    ?.uncaughtException(Thread.currentThread(), e)
                return@setErrorHandler
            }

            log.error(e) { "undeliverable_rx_exception" }
        }
    }

    private fun initLogging() {
        HandroidLoggerAdapter.APP_NAME = ""
        HandroidLoggerAdapter.DEBUG = BuildConfig.DEBUG
        HandroidLoggerAdapter.ANDROID_API_LEVEL = Build.VERSION.SDK_INT
    }
}