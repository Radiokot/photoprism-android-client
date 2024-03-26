package ua.com.radiokot.photoprism.features.ext.data.storage

import android.content.SharedPreferences
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import ua.com.radiokot.photoprism.base.data.storage.ObjectPersistence
import ua.com.radiokot.photoprism.base.data.storage.ObjectPersistenceOnPrefs
import ua.com.radiokot.photoprism.di.JsonObjectMapper
import ua.com.radiokot.photoprism.features.ext.data.model.ActivatedGalleryExtension
import ua.com.radiokot.photoprism.features.ext.data.model.GalleryExtension
import ua.com.radiokot.photoprism.features.ext.data.model.GalleryExtensionsState
import java.util.Date

class GalleryExtensionsStatePersistenceOnPrefs(
    key: String,
    preferences: SharedPreferences,
    jsonObjectMapper: JsonObjectMapper,
) : ObjectPersistence<GalleryExtensionsState> {
    private val underlyingPersistence =
        ObjectPersistenceOnPrefs.forType<StoredGalleryExtensionsState>(
            key,
            preferences,
            jsonObjectMapper
        )

    override fun loadItem(): GalleryExtensionsState? =
        underlyingPersistence.loadItem()?.toSource()

    override fun saveItem(item: GalleryExtensionsState) =
        underlyingPersistence.saveItem(StoredGalleryExtensionsState(item))

    override fun hasItem(): Boolean =
        underlyingPersistence.hasItem()

    override fun clear() =
        underlyingPersistence.clear()

    private class StoredGalleryExtensionsState
    @JsonCreator
    constructor(
        @JsonProperty("ps")
        val primarySubject: String?,
        @JsonProperty("ae")
        val activatedExtensions: List<StoredActivatedGalleryExtension>
    ) {
        constructor(source: GalleryExtensionsState) : this(
            primarySubject = source.primarySubject,
            activatedExtensions = source.activatedExtensions.map(::StoredActivatedGalleryExtension),
        )

        fun toSource() = GalleryExtensionsState(
            primarySubject = primarySubject,
            activatedExtensions = activatedExtensions.map(StoredActivatedGalleryExtension::toSource),
        )

        class StoredActivatedGalleryExtension
        @JsonCreator
        constructor(
            @JsonProperty("t")
            val type: GalleryExtension,
            @JsonProperty("k")
            val key: String,
            @JsonProperty("e")
            val expiresAtMs: Long?,
        ) {
            constructor(source: ActivatedGalleryExtension) : this(
                type = source.type,
                key = source.key,
                expiresAtMs = source.expiresAt?.time,
            )

            fun toSource() = ActivatedGalleryExtension(
                type = type,
                key = key,
                expiresAt = expiresAtMs?.let(::Date),
            )
        }
    }
}
