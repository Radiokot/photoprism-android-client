package ua.com.radiokot.photoprism.features.gallery.folders.data.storage

import android.content.SharedPreferences
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import ua.com.radiokot.photoprism.base.data.storage.ObjectPersistence
import ua.com.radiokot.photoprism.base.data.storage.ObjectPersistenceOnPrefs
import ua.com.radiokot.photoprism.di.JsonObjectMapper
import ua.com.radiokot.photoprism.features.albums.view.model.AlbumSort

class AlbumSortPersistenceOnPrefs(
    key: String,
    preferences: SharedPreferences,
    jsonObjectMapper: JsonObjectMapper,
) : ObjectPersistence<AlbumSort> {

    private val v1Persistence =
        ObjectPersistenceOnPrefs.forType<StoredAlbumSortV1>(
            key + 1,
            preferences,
            jsonObjectMapper
        )

    private val underlyingPersistence: ObjectPersistenceOnPrefs<StoredAlbumSortV1> by lazy {
        // Add migration here in the future.
        v1Persistence
    }

    override fun loadItem(): AlbumSort? =
        underlyingPersistence.loadItem()?.toSource()

    override fun hasItem(): Boolean =
        underlyingPersistence.hasItem()

    override fun saveItem(item: AlbumSort) =
        underlyingPersistence.saveItem(StoredAlbumSortV1(item))

    override fun clear() =
        underlyingPersistence.clear()

    private class StoredAlbumSortV1
    @JsonCreator
    constructor(
        @JsonProperty("o")
        val order: AlbumSort.Order,
        @JsonProperty("ff")
        val areFavoritesFirst: Boolean,
    ) {
        constructor(source: AlbumSort) : this(
            order = source.order,
            areFavoritesFirst = source.areFavoritesFirst,
        )

        fun toSource() = AlbumSort(
            order = this.order,
            areFavoritesFirst = this.areFavoritesFirst,
        )
    }
}
