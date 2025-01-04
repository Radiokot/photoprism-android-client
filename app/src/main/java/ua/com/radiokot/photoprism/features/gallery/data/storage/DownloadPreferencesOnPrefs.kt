package ua.com.radiokot.photoprism.features.gallery.data.storage

import android.content.SharedPreferences
import io.reactivex.rxjava3.subjects.BehaviorSubject
import ua.com.radiokot.photoprism.extension.kLogger
import ua.com.radiokot.photoprism.extension.tryOrNull
import ua.com.radiokot.photoprism.util.booleanPreferenceSubject
import ua.com.radiokot.photoprism.util.stringPreferenceSubject
import ua.com.radiokot.photoprism.util.stringifyPreferenceSubject


class DownloadPreferencesOnPrefs(
    preferences: SharedPreferences,
) : DownloadPreferences {
    private val log = kLogger("GalleryPreferencesOnPrefs")

    override val downloadDirEn: BehaviorSubject<Boolean> =
        booleanPreferenceSubject(
            preferences = preferences,
            key = "download_folder_en",
            defaultValue = false
        )

    override val downloadDirName: BehaviorSubject<String> =
        stringPreferenceSubject(
            preferences = preferences,
            key = "download_folder_name",
            defaultValue = "PhotoprismDL",
            onValuePut = { _: String, newValue: String ->
                log.debug {
                    "livePhotosAsImages::onNext(): set_value:" +
                            "\nvalue=$newValue"
                }
            }
        )
    }
