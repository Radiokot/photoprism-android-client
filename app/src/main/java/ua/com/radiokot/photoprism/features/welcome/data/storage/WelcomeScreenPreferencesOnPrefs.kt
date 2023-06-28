package ua.com.radiokot.photoprism.features.welcome.data.storage

import android.content.SharedPreferences
import androidx.core.content.edit
import ua.com.radiokot.photoprism.extension.kLogger

class WelcomeScreenPreferencesOnPrefs(
    private val preferences: SharedPreferences,
    keyPrefix: String = "welcome"
) : WelcomeScreenPreferences {
    private val log = kLogger("WelcomeScreenPreferencesOnPrefs")

    private val welcomeNoticeAcceptedKey = "${keyPrefix}_is_notice_accepted"

    override var isWelcomeNoticeAccepted: Boolean
        get() = preferences.getBoolean(welcomeNoticeAcceptedKey, false)
        set(value) = preferences.edit { putBoolean(welcomeNoticeAcceptedKey, value) }
            .also {
                log.debug {
                    "isWelcomeNoticeAccepted::set(): set_value:" +
                            "\nvalue=$value"
                }
            }
}
