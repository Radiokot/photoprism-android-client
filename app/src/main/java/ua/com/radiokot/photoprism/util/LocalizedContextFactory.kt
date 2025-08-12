package ua.com.radiokot.photoprism.util

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import androidx.core.os.ConfigurationCompat
import ua.com.radiokot.photoprism.R
import ua.com.radiokot.photoprism.extension.checkNotNull
import java.util.Locale

/**
 * @param base base context to override during the attachment
 */
class LocalizedContextFactory(
    private val base: Context,
) {
    private fun getLocaleOfStrings(resources: Resources): Locale {
        val configurationLocales = ConfigurationCompat.getLocales(resources.configuration)
        return Locale.Builder()
            .setLanguage(resources.getString(R.string.language_code))
            .setScript(resources.getString(R.string.script_code).takeUnless(String::isEmpty))
            .setRegion(configurationLocales.get(0).checkNotNull().country)
            .build()
    }

    /**
     * @return context having localized resource configuration, to be attached as a base one.
     *
     * @param locale desired locale. By default is determined by [R.string.language_code]
     * from the [base] resources configured by the OS to one of the supported languages or
     * the default one (English).
     */
    fun getLocalizedContext(
        locale: Locale = getLocaleOfStrings(base.resources),
    ): Context =
        base.createConfigurationContext(Configuration().apply {
            setLocale(locale)
        })
}
