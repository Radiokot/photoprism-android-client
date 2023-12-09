package ua.com.radiokot.photoprism.features.viewer.slideshow.data.storage

import android.content.SharedPreferences
import androidx.core.content.edit
import ua.com.radiokot.photoprism.extension.kLogger

class SlideshowPreferencesOnPrefs(
    private val preferences: SharedPreferences,
    keyPrefix: String = "slideshow"
) : SlideshowPreferences {
    private val log = kLogger("SlideshowPreferencesOnPrefs")

    private val guideAcceptedKey = "${keyPrefix}_is_guide_accepted"

    override var isGuideAccepted: Boolean
        get() = preferences.getBoolean(guideAcceptedKey, false)
        set(value) = preferences.edit { putBoolean(guideAcceptedKey, value) }
            .also {
                log.debug {
                    "isGuideAccepted::set(): set_value:" +
                            "\nvalue=$value"
                }
            }
}
