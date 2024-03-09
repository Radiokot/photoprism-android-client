package ua.com.radiokot.photoprism.features.ext.memories.data.storage

import android.content.SharedPreferences
import com.fasterxml.jackson.annotation.JsonProperty
import ua.com.radiokot.photoprism.base.data.storage.ObjectPersistence
import ua.com.radiokot.photoprism.base.data.storage.ObjectPersistenceOnPrefs
import ua.com.radiokot.photoprism.di.JsonObjectMapper
import ua.com.radiokot.photoprism.features.ext.memories.logic.UpdateMemoriesWorker

class UpdateMemoriesWorkerStatusPersistenceOnPrefs(
    key: String,
    preferences: SharedPreferences,
    jsonObjectMapper: JsonObjectMapper,
) :
    ObjectPersistence<UpdateMemoriesWorker.Status> {
    private val underlyingPersistence =
        ObjectPersistenceOnPrefs.forType<StoredStatus>(key, preferences, jsonObjectMapper)

    override fun loadItem(): UpdateMemoriesWorker.Status? =
        underlyingPersistence.loadItem()?.toStatus()

    override fun hasItem(): Boolean =
        underlyingPersistence.hasItem()

    override fun clear() =
        underlyingPersistence.clear()

    override fun saveItem(item: UpdateMemoriesWorker.Status) =
        underlyingPersistence.saveItem(StoredStatus.fromStatus(item))

    private class StoredStatus(
        @JsonProperty("d")
        val lastSuccessfulUpdateDay: Int,
    ) {
        fun toStatus() = UpdateMemoriesWorker.Status(
            lastSuccessfulUpdateDay = lastSuccessfulUpdateDay,
        )

        companion object {
            fun fromStatus(status: UpdateMemoriesWorker.Status) = StoredStatus(
                lastSuccessfulUpdateDay = status.lastSuccessfulUpdateDay,
            )
        }
    }
}
