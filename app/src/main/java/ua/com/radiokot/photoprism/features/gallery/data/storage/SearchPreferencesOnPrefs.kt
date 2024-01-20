package ua.com.radiokot.photoprism.features.gallery.data.storage

import android.content.SharedPreferences
import io.reactivex.rxjava3.subjects.BehaviorSubject
import ua.com.radiokot.photoprism.extension.kLogger
import ua.com.radiokot.photoprism.util.booleanPreferenceSubject

class SearchPreferencesOnPrefs(
    private val preferences: SharedPreferences,
    private val keyPrefix: String = "search",
) : SearchPreferences {
    private val log = kLogger("SearchPreferencesOnPrefs")

    private fun getBooleanSubject(localKey: String): BehaviorSubject<Boolean> =
        booleanPreferenceSubject(
            preferences = preferences,
            key = "${keyPrefix}_$localKey",
            defaultValue = true,
            onValuePut = { key, newValue ->
                log.debug {
                    "onValuePut(): set_value:" +
                            "\nkey=$key" +
                            "\nvalue=$newValue"
                }
            }
        )

    override val showPeople: BehaviorSubject<Boolean> =
        getBooleanSubject("show_people")

    override val showAlbums: BehaviorSubject<Boolean> =
        getBooleanSubject("show_albums")

    override val showAlbumFolders: BehaviorSubject<Boolean> =
        getBooleanSubject("show_album_folders")
}
