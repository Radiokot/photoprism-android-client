package ua.com.radiokot.photoprism.features.gallery.folders.data.storage

import android.content.SharedPreferences
import androidx.core.content.edit
import io.reactivex.rxjava3.subjects.BehaviorSubject
import ua.com.radiokot.photoprism.di.JsonObjectMapper
import ua.com.radiokot.photoprism.features.albums.view.model.AlbumSort
import ua.com.radiokot.photoprism.util.objectPersistenceSubject

class GalleryAlbumsPreferencesOnPrefs(
    defaultSort: AlbumSort,
    preferences: SharedPreferences,
    keyPrefix: String = "albums",
    jsonObjectMapper: JsonObjectMapper,
) : GalleryAlbumsPreferences {
    init {
        val folderSortKeyV1 = "folders_sort1"
        val folderSortKeyV2 = "${keyPrefix}_folder_sort1"
        preferences.getString(folderSortKeyV1, null)?.also { folderSort ->
            preferences.edit {
                putString(folderSortKeyV2, folderSort)
                putString(folderSortKeyV1, null)
            }
        }
    }

    override val folderSort: BehaviorSubject<AlbumSort> =
        objectPersistenceSubject(
            persistence = AlbumSortPersistenceOnPrefs(
                key = "${keyPrefix}_folder_sort",
                preferences = preferences,
                jsonObjectMapper = jsonObjectMapper,
            ),
            defaultValue = defaultSort,
        )

    override val albumSort: BehaviorSubject<AlbumSort> =
        objectPersistenceSubject(
            persistence = AlbumSortPersistenceOnPrefs(
                key = "${keyPrefix}_album_sort",
                preferences = preferences,
                jsonObjectMapper = jsonObjectMapper,
            ),
            defaultValue = defaultSort,
        )
}
