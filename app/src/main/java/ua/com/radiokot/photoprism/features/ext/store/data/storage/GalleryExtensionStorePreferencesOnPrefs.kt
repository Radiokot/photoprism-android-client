package ua.com.radiokot.photoprism.features.ext.store.data.storage

import android.content.SharedPreferences
import androidx.core.content.edit
import ua.com.radiokot.photoprism.extension.kLogger

class GalleryExtensionStorePreferencesOnPrefs(
    private val preferences: SharedPreferences,
    keyPrefix: String = "extension_store"
) : GalleryExtensionStorePreferences {
    private val log = kLogger("GalleryExtensionStorePreferencesOnPrefs")

    private val disclaimerAcceptedKey = "${keyPrefix}_is_disclaimer_accepted"

    override var isDisclaimerAccepted: Boolean
        get() = preferences.getBoolean(disclaimerAcceptedKey, false)
        set(value) = preferences.edit { putBoolean(disclaimerAcceptedKey, value) }
            .also {
                log.debug {
                    "isDisclaimerAccepted::set(): set_value:" +
                            "\nvalue=$value"
                }
            }
}
