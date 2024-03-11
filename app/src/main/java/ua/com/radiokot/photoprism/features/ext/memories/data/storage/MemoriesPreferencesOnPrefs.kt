package ua.com.radiokot.photoprism.features.ext.memories.data.storage

import android.content.SharedPreferences
import androidx.core.content.edit
import ua.com.radiokot.photoprism.extension.kLogger

class MemoriesPreferencesOnPrefs(
    private val preferences: SharedPreferences,
    keyPrefix: String,
) : MemoriesPreferences {
    private val log = kLogger("MemoriesPreferencesOnPrefs")

    private val notificationsEnabledKey = "${keyPrefix}_notifications_enabled"
    override var areNotificationsEnabled: Boolean
        // Set enabled by default to match the behavior of notification channels.
        get() = preferences.getBoolean(notificationsEnabledKey, true)
        set(value) = preferences.edit { putBoolean(notificationsEnabledKey, value) }
            .also {
                log.debug {
                    "areNotificationsEnabled::set(): set_value:" +
                            "\nvalue=$value"
                }
            }

}
