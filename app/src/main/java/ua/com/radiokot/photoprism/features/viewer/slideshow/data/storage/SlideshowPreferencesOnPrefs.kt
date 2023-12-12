package ua.com.radiokot.photoprism.features.viewer.slideshow.data.storage

import android.content.SharedPreferences
import androidx.core.content.edit
import ua.com.radiokot.photoprism.extension.kLogger
import ua.com.radiokot.photoprism.extension.tryOrNull
import ua.com.radiokot.photoprism.features.viewer.slideshow.data.model.SlideshowSpeed

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

    private val speedKey = "${keyPrefix}_speed"
    override var speed: SlideshowSpeed
        get() = preferences.getString(speedKey, null)
            .let { savedStringValue ->
                tryOrNull {
                    SlideshowSpeed.valueOf(savedStringValue!!)
                } ?: SlideshowSpeed.NORMAL
            }
        set(value) = preferences.edit { putString(speedKey, value.name) }
            .also {
                log.debug {
                    "speed::set(): set_value:" +
                            "\nvalue=$value"
                }
            }
}
