package ua.com.radiokot.photoprism.features.gallery.folders.data.storage

import android.content.SharedPreferences
import io.reactivex.rxjava3.subjects.BehaviorSubject
import ua.com.radiokot.photoprism.di.JsonObjectMapper
import ua.com.radiokot.photoprism.features.albums.view.model.AlbumSort
import ua.com.radiokot.photoprism.util.objectPersistenceSubject

class GalleryFoldersPreferencesOnPrefs(
    defaultSort: AlbumSort,
    preferences: SharedPreferences,
    keyPrefix: String = "folders",
    jsonObjectMapper: JsonObjectMapper,
) : GalleryFoldersPreferences {
    override val sort: BehaviorSubject<AlbumSort> =
        objectPersistenceSubject(
            persistence = AlbumSortPersistenceOnPrefs(
                key = "${keyPrefix}_sort",
                preferences = preferences,
                jsonObjectMapper = jsonObjectMapper,
            ),
            defaultValue = defaultSort,
        )
}
