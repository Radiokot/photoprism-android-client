package ua.com.radiokot.photoprism.features.gallery.data.storage

import android.annotation.SuppressLint
import android.content.SharedPreferences
import androidx.core.content.edit
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.subjects.BehaviorSubject
import ua.com.radiokot.photoprism.extension.kLogger

class SearchPreferencesOnPrefs(
    private val preferences: SharedPreferences,
    keyPrefix: String = "search",
) : SearchPreferences {
    private val log = kLogger("SearchPreferencesOnPrefs")

    @SuppressLint("CheckResult")
    private fun getBooleanSubject(
        name: String,
        key: String,
        default: Boolean,
    ): BehaviorSubject<Boolean> =
        BehaviorSubject.createDefault(preferences.getBoolean(key, default)).apply {
            skip(1).subscribeBy { newValue ->
                log.debug {
                    "$name::onNext(): set_value:" +
                            "\nvalue=$newValue"
                }

                preferences.edit {
                    putBoolean(key, newValue)
                }
            }
        }

    override val showPeople: BehaviorSubject<Boolean> =
        getBooleanSubject(
            name = "showPeople",
            key = "${keyPrefix}_show_albums",
            default = true,
        )

    @SuppressLint("CheckResult")
    override val showAlbums: BehaviorSubject<Boolean> =
        getBooleanSubject(
            name = "showAlbums",
            key = "${keyPrefix}_show_albums",
            default = true,
        )

    @SuppressLint("CheckResult")
    override val showAlbumFolders: BehaviorSubject<Boolean> =
        getBooleanSubject(
            name = "showAlbumFolders",
            key = "${keyPrefix}_show_album_folders",
            default = true,
        )
}
