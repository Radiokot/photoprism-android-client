package ua.com.radiokot.photoprism.features.gallery.data.storage

import android.content.SharedPreferences
import io.reactivex.rxjava3.subjects.BehaviorSubject
import ua.com.radiokot.photoprism.features.gallery.data.model.RawDownloadMode
import ua.com.radiokot.photoprism.util.booleanPreferenceSubject
import ua.com.radiokot.photoprism.util.stringifyPreferenceSubject

class DownloadPreferencesOnPrefs(
    preferences: SharedPreferences,
) : DownloadPreferences {
    override val useSeparateFolder: BehaviorSubject<Boolean> =
        booleanPreferenceSubject(
            preferences = preferences,
            key = "download_use_separate_folder",
            defaultValue = false,
        )

    override val separateFolderName: BehaviorSubject<String> =
        stringifyPreferenceSubject(
            preferences = preferences,
            key = "download_separate_folder_name",
            defaultValue = "PhotoPrism",
            stringSerializer = String::toString,
            stringDeserializer = String::toString,
        )

    override val rawDownloadMode: BehaviorSubject<RawDownloadMode> =
        stringifyPreferenceSubject(
            preferences = preferences,
            key = "download_raw_mode",
            defaultValue = RawDownloadMode.ORIGINAL,
            stringSerializer = RawDownloadMode::name,
            stringDeserializer = RawDownloadMode::valueOf,
        )
}
