package ua.com.radiokot.photoprism

import android.app.Application
import android.content.Context
import android.os.Build
import androidx.core.os.ConfigurationCompat
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
import ua.com.radiokot.photoprism.di.appDbModule
import ua.com.radiokot.photoprism.di.devDbModule
import ua.com.radiokot.photoprism.di.memoryOnlyDevDbModule
import ua.com.radiokot.photoprism.di.retrofitApiModule
import ua.com.radiokot.photoprism.env.data.model.EnvSession
import ua.com.radiokot.photoprism.env.data.storage.EnvSessionHolder
import ua.com.radiokot.photoprism.extension.kLogger
import ua.com.radiokot.photoprism.featureflags.di.featureFlagsModule
import ua.com.radiokot.photoprism.featureflags.di.noOpFeatureFlagsModule
import ua.com.radiokot.photoprism.featureflags.extension.hasMemoriesExtension
import ua.com.radiokot.photoprism.featureflags.logic.FeatureFlags
import ua.com.radiokot.photoprism.features.envconnection.di.envConnectionFeatureModule
import ua.com.radiokot.photoprism.features.ext.key.input.di.keyInputFeatureModule
import ua.com.radiokot.photoprism.features.ext.memories.di.memoriesFeatureModule
import ua.com.radiokot.photoprism.features.ext.memories.logic.CancelDailyMemoriesUpdatesUseCase
import ua.com.radiokot.photoprism.features.ext.memories.logic.ScheduleDailyMemoriesUpdatesUseCase
import ua.com.radiokot.photoprism.features.gallery.di.galleryFeatureModule
import ua.com.radiokot.photoprism.features.viewer.di.mediaViewerFeatureModule
import ua.com.radiokot.photoprism.features.viewer.slideshow.di.slideshowFeatureModule
import ua.com.radiokot.photoprism.features.webview.di.webViewFeatureModule
import ua.com.radiokot.photoprism.features.welcome.di.welcomeScreenFeatureModule
import ua.com.radiokot.photoprism.util.LocalizedContextFactory
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
                retrofitApiModule
                        + appDbModule
                        + (if (BuildConfig.DEBUG) devDbModule else memoryOnlyDevDbModule)
                        + (if (BuildConfig.DEBUG) featureFlagsModule else noOpFeatureFlagsModule)

                        + galleryFeatureModule
                        + mediaViewerFeatureModule
                        + envConnectionFeatureModule
                        + webViewFeatureModule
                        + welcomeScreenFeatureModule
                        + slideshowFeatureModule
                        + memoriesFeatureModule
                        + keyInputFeatureModule
            )
            androidFileProperties("app.properties")
        }

        initRxErrorHandler()
        initLogging()
        initOptionalFeatures()

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

    private fun initOptionalFeatures() = with(get<FeatureFlags>()) {
        if (hasMemoriesExtension) {
            get<ScheduleDailyMemoriesUpdatesUseCase>().invoke()
        } else {
            get<CancelDailyMemoriesUpdatesUseCase>().invoke()
        }
    }

    override fun attachBaseContext(base: Context) =
        super.attachBaseContext(
            LocalizedContextFactory(base)
                .getLocalizedContext()
                .also { newBase ->
                    ConfigurationCompat.getLocales(newBase.resources.configuration)[0]
                        ?.also(Locale::setDefault)
                }
        )

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
