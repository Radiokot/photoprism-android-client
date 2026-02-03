package ua.com.radiokot.photoprism.features.map.data.storage

import android.content.SharedPreferences
import androidx.core.content.edit

class MapPreferencesOnPrefs(
    private val preferences: SharedPreferences,
    keyPrefix: String,
) : MapPreferences {

    private val customStyleUrlKey = "${keyPrefix}_custom_style_url"
    override var customStyleUrl: String?
        get() = preferences.getString(customStyleUrlKey, null)
        set(value) {
            preferences.edit {
                putString(customStyleUrlKey, value)
            }
        }
}
