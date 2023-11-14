package ua.com.radiokot.photoprism

import android.app.Application
import android.content.Context
import android.os.Build
import io.reactivex.rxjava3.exceptions.UndeliverableException
import io.reactivex.rxjava3.plugins.RxJavaPlugins
import org.koin.android.ext.android.get
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidFileProperties
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.qualifier._q
import org.koin.core.qualifier.named
import org.slf4j.impl.HandroidLoggerAdapter
import ua.com.radiokot.photoprism.base.data.storage.ObjectPersistence
import ua.com.radiokot.photoprism.di.INTERNAL_DOWNLOADS_DIRECTORY
import ua.com.radiokot.photoprism.di.dbModules
import ua.com.radiokot.photoprism.di.retrofitApiModules
import ua.com.radiokot.photoprism.env.data.model.EnvSession
import ua.com.radiokot.photoprism.env.data.storage.EnvSessionHolder
import ua.com.radiokot.photoprism.extension.kLogger
import ua.com.radiokot.photoprism.features.envconnection.di.envConnectionFeatureModules
import ua.com.radiokot.photoprism.features.gallery.di.galleryFeatureModules
import ua.com.radiokot.photoprism.features.viewer.slideshow.di.slideshowFeatureModules
import ua.com.radiokot.photoprism.features.viewer.di.mediaViewerFeatureModules
import ua.com.radiokot.photoprism.features.webview.di.webViewFeatureModules
import ua.com.radiokot.photoprism.features.welcome.di.welcomeScreenFeatureModules
import ua.com.radiokot.photoprism.util.LocalizationHelper
import java.io.File
import java.io.IOException
import java.util.Locale
import kotlin.concurrent.thread

class PhotoPrismGallery : Application() {
    private val log = kLogger("App")

    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidLogger()
            androidContext(this@PhotoPrismGallery)
            modules(
                retrofitApiModules
                        + dbModules
                        + galleryFeatureModules
                        + mediaViewerFeatureModules
                        + envConnectionFeatureModules
                        + webViewFeatureModules
                        + welcomeScreenFeatureModules
                        + slideshowFeatureModules
            )
            androidFileProperties("app.properties")
        }

        initRxErrorHandler()
        initLogging()

        loadSessionIfPresent()
        clearInternalDownloads()
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
        HandroidLoggerAdapter.APP_NAME = "PPG"
        HandroidLoggerAdapter.DEBUG = BuildConfig.DEBUG
        HandroidLoggerAdapter.ANDROID_API_LEVEL = Build.VERSION.SDK_INT
    }

    override fun attachBaseContext(newBase: Context) {
        val stringsLocale = LocalizationHelper.getLocaleOfStrings(newBase.resources)
        Locale.setDefault(stringsLocale)
        super.attachBaseContext(
            LocalizationHelper.getLocalizedConfigurationContext(
                context = newBase,
                locale = stringsLocale,
            )
        )
    }

    private fun loadSessionIfPresent() {
        val sessionPersistence =
            get<ObjectPersistence<EnvSession>>(_q<EnvSession>())
        val sessionHolder = get<EnvSessionHolder>()
        val loadedSession = sessionPersistence
            .loadItem()

        if (loadedSession != null) {
            sessionHolder.set(loadedSession)
            log.debug { "loadSessionIfPresent(): loaded_session_from_persistence" }
        } else {
            log.debug { "loadSessionIfPresent(): no_session_found_in_persistence" }
        }
    }

    private fun clearInternalDownloads() {
        thread {
            try {
                get<File>(named(INTERNAL_DOWNLOADS_DIRECTORY))
                    .listFiles()
                    ?.forEach(File::deleteRecursively)
            } catch (e: Throwable) {
                log.error(e) { "clearInternalDownloads(): error_occurred" }
            }
        }
    }
}
