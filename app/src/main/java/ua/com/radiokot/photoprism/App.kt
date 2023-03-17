package ua.com.radiokot.photoprism

import android.app.Application
import android.content.Context
import android.os.Build
import io.reactivex.rxjava3.exceptions.UndeliverableException
import io.reactivex.rxjava3.plugins.RxJavaPlugins
import org.koin.android.ext.android.get
import org.koin.android.ext.android.getKoin
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidFileProperties
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.qualifier.named
import org.koin.core.qualifier.qualifier
import org.slf4j.impl.HandroidLoggerAdapter
import ua.com.radiokot.photoprism.base.data.storage.ObjectPersistence
import ua.com.radiokot.photoprism.di.DI_SCOPE_SESSION
import ua.com.radiokot.photoprism.di.retrofitApiModules
import ua.com.radiokot.photoprism.extension.kLogger
import ua.com.radiokot.photoprism.features.env.data.model.PhotoPrismSession
import ua.com.radiokot.photoprism.features.env.di.envFeatureModules
import ua.com.radiokot.photoprism.features.gallery.di.galleryFeatureModules
import ua.com.radiokot.photoprism.features.viewer.di.mediaViewerFeatureModules
import ua.com.radiokot.photoprism.util.LocalizationHelper
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
                        + envFeatureModules
            )
            properties(
                mapOf(
                    "apiUrl" to BuildConfig.API_URL
                )
            )
            androidFileProperties("app.properties")
        }

        loadSessionIfPresent()

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

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(
            LocalizationHelper.getLocalizedConfigurationContext(
                context = newBase,
                locale = LocalizationHelper.getLocaleOfStrings(newBase.resources),
            )
        )
    }

    private fun loadSessionIfPresent() {
        val sessionPersistence =
            get<ObjectPersistence<PhotoPrismSession>>(qualifier<PhotoPrismSession>())
        sessionPersistence.saveItem(
            PhotoPrismSession(
                apiUrl = BuildConfig.API_URL,
                id = BuildConfig.SESSION_ID,
                downloadToken = "bvmk6aml",
                previewToken = "3hjej82k",
            )
        )
        val session = sessionPersistence.loadItem()
//        if (session != null) {
//            getKoin().createScope(
//                DI_SCOPE_SESSION,
//                named<PhotoPrismSession>(),
//                session,
//            )
//        }
    }
}