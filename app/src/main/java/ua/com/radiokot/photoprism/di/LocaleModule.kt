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
        // This method works in combination with
        // .attachBaseContext hooks and is able to detect either
        // the locale of currently picked resources or the one
        // selected in Android 13 language preferences.
        //
        // The combination looks complicated, yet trying to simplify it
        // so far led to locale bugs on either new or old Android versions.
        val application = androidApplication()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            (application.getSystemService<LocaleManager>()?.applicationLocales
                ?: application.resources.configuration.locales)[0]
                ?: Locale.getDefault()
        } else {
            ConfigurationCompat.getLocales(application.resources.configuration)[0]
                ?: Locale.getDefault()
        }
    } bind Locale::class
}
