package ua.com.radiokot.photoprism.features.prefs.extension

import androidx.annotation.StringRes
import androidx.preference.Preference
import androidx.preference.PreferenceScreen
import ua.com.radiokot.photoprism.extension.checkNotNull

fun PreferenceScreen.requirePreference(@StringRes keyId: Int): Preference {
    val key = context.getString(keyId)
    return findPreference<Preference>(key).checkNotNull {
        "Required preference '$key' not found"
    }
}
