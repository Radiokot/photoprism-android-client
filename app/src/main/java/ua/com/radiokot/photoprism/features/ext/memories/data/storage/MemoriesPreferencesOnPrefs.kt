package ua.com.radiokot.photoprism.features.ext.memories.data.storage

import android.content.SharedPreferences
import androidx.core.content.edit
import io.reactivex.rxjava3.subjects.BehaviorSubject
import ua.com.radiokot.photoprism.extension.kLogger
import ua.com.radiokot.photoprism.util.booleanPreferenceSubject

class MemoriesPreferencesOnPrefs(
    private val preferences: SharedPreferences,
    keyPrefix: String,
) : MemoriesPreferences {
    private val log = kLogger("MemoriesPreferencesOnPrefs")

    override val isEnabled: BehaviorSubject<Boolean> =
        booleanPreferenceSubject(
            preferences = preferences,
            key = "${keyPrefix}_is_enabled",
            defaultValue = true,
            onValuePut = { _, newValue ->
                log.debug {
                    "isEnabled::onNext(): set_value:" +
                            "\nvalue=$newValue"
                }
            }
        )

    private val maxEntriesInMemoryKey = "${keyPrefix}_memory_max_entries"
    override var maxEntriesInMemory: Int
        get() = preferences.getInt(maxEntriesInMemoryKey, 6)
        set(value) {
            require(value > 0) {
                "The number be positive"
            }

            preferences.edit { putInt(maxEntriesInMemoryKey, value) }
                .also {
                    log.debug {
                        "maxEntriesInMemory::set(): set_value:" +
                                "\nvalue=$value"
                    }
                }
        }

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
