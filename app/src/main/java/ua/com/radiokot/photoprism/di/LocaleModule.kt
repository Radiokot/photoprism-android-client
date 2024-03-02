package ua.com.radiokot.photoprism.di

import android.app.LocaleManager
import android.os.Build
import androidx.core.content.getSystemService
import androidx.core.os.ConfigurationCompat
import org.koin.android.ext.koin.androidApplication
import org.koin.dsl.bind
import org.koin.dsl.module
import java.util.Locale

val localeModule = module {
    // Primary locale.
    factory {
        val application = androidApplication()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            (application.getSystemService<LocaleManager>()?.applicationLocales
                ?: application.resources.configuration.locales)[0]
                ?: Locale.getDefault()
        } else {
            ConfigurationCompat.getLocales(application.resources.configuration)[0]
                ?: Locale.getDefault()
        }
            .also { println("OOLEG here $it") }
    } bind Locale::class
}
