package ua.com.radiokot.photoprism.util

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import androidx.core.os.ConfigurationCompat
import ua.com.radiokot.photoprism.R
import ua.com.radiokot.photoprism.extension.checkNotNull
import java.util.*

object LocalizationHelper {
    fun getLocaleOfStrings(resources: Resources): Locale {
        val configurationLocales = ConfigurationCompat.getLocales(resources.configuration)
        val stringsLanguageCode = resources.getString(R.string.language_code)
        return Locale.Builder()
            .setLanguage(stringsLanguageCode)
            .setRegion(configurationLocales.get(0).checkNotNull().country)
            .build()
    }

    fun getLocalizedConfigurationContext(context: Context, locale: Locale): Context =
        context.createConfigurationContext(Configuration().apply {
            setLocale(locale)
        })
}