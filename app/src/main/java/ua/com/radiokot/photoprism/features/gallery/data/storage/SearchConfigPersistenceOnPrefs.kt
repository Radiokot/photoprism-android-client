package ua.com.radiokot.photoprism.features.gallery.data.storage

import android.content.SharedPreferences
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import ua.com.radiokot.photoprism.base.data.storage.ObjectPersistence
import ua.com.radiokot.photoprism.base.data.storage.ObjectPersistenceOnPrefs
import ua.com.radiokot.photoprism.di.JsonObjectMapper
import ua.com.radiokot.photoprism.features.gallery.data.model.GalleryMedia
import ua.com.radiokot.photoprism.features.gallery.data.model.SearchConfig

class SearchConfigPersistenceOnPrefs(
    key: String,
    preferences: SharedPreferences,
    jsonObjectMapper: JsonObjectMapper,
) : ObjectPersistence<SearchConfig> {

    private val v1Persistence =
        ObjectPersistenceOnPrefs.forType<StoredSearchConfigV1>(
            key + 1,
            preferences,
            jsonObjectMapper
        )

    private val underlyingPersistence: ObjectPersistenceOnPrefs<StoredSearchConfigV1> by lazy {
        // Add migration here in the future.
        v1Persistence
    }

    override fun loadItem(): SearchConfig? =
        underlyingPersistence.loadItem()?.toSource()

    override fun hasItem(): Boolean =
        underlyingPersistence.hasItem()

    override fun saveItem(item: SearchConfig) =
        underlyingPersistence.saveItem(StoredSearchConfigV1(item))

    override fun clear() =
        underlyingPersistence.clear()

    private class StoredSearchConfigV1
    @JsonCreator
    constructor(
        @JsonProperty("q")
        val userQuery: String,
        @JsonProperty("mt")
        val mediaTypes: List<String>?,
        @JsonProperty("ip")
        val includePrivate: Boolean,
        @JsonProperty("of")
        val onlyFavorite: Boolean?,
        @JsonProperty("a")
        val albumUid: String?,
        @JsonProperty("pe")
        val personIds: Set<String>,
    ) {
        constructor(source: SearchConfig) : this(
            userQuery = source.userQuery,
            mediaTypes = source.mediaTypes
                ?.map(GalleryMedia.TypeName::toString),
            includePrivate = source.includePrivate,
            onlyFavorite = source.onlyFavorite,
            albumUid = source.albumUid,
            personIds = source.personIds,
        )

        fun toSource() = SearchConfig(
            userQuery = userQuery,
            mediaTypes = mediaTypes
                ?.map { GalleryMedia.TypeName.valueOf(it) }
                ?.toSet(),
            includePrivate = includePrivate,
            onlyFavorite = onlyFavorite == true,
            beforeLocal = null,
            afterLocal = null,
            albumUid = albumUid,
            personIds = personIds,
        )
    }
}
