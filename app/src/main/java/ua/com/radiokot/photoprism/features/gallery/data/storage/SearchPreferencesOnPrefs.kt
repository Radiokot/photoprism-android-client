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

    private val showPeopleKey = "${keyPrefix}_show_people"
    private val showAlbumsKey = "${keyPrefix}_show_albums"

    @SuppressLint("CheckResult")
    override val showPeople =
        BehaviorSubject.createDefault(
            preferences.getBoolean(showPeopleKey, true)
        ).apply {
            skip(1).subscribeBy { newValue ->
                log.debug {
                    "showPeople::onNext(): set_value:" +
                            "\nvalue=$newValue"
                }

                preferences.edit {
                    putBoolean(showPeopleKey, newValue)
                }
            }
        }

    @SuppressLint("CheckResult")
    override val showAlbums =
        BehaviorSubject.createDefault(
            preferences.getBoolean(showAlbumsKey, true)
        ).apply {
            skip(1).subscribeBy { newValue ->
                log.debug {
                    "showAlbums::onNext(): set_value:" +
                            "\nvalue=$newValue"
                }

                preferences.edit {
                    putBoolean(showAlbumsKey, newValue)
                }
            }
        }
}
