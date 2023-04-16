package ua.com.radiokot.photoprism.features.envconnection.data.storage

import android.content.SharedPreferences
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import ua.com.radiokot.photoprism.base.data.storage.ObjectPersistence
import ua.com.radiokot.photoprism.base.data.storage.ObjectPersistenceOnPrefs
import ua.com.radiokot.photoprism.di.JsonObjectMapper
import ua.com.radiokot.photoprism.env.data.model.EnvConnectionParams
import ua.com.radiokot.photoprism.env.data.model.EnvSession
import ua.com.radiokot.photoprism.extension.tryOrNull

class EnvSessionPersistenceOnPrefs(
    key: String,
    preferences: SharedPreferences,
    jsonObjectMapper: JsonObjectMapper,
) : ObjectPersistence<EnvSession> {
    private val underlyingPersistence =
        ObjectPersistenceOnPrefs.forType<StoredEnvSession>(key, preferences, jsonObjectMapper)

    override fun loadItem(): EnvSession? = tryOrNull {
        underlyingPersistence.loadItem()?.toSource()
    }

    override fun saveItem(item: EnvSession) =
        underlyingPersistence.saveItem(StoredEnvSession(item))

    override fun hasItem(): Boolean =
        underlyingPersistence.hasItem()

    override fun clear() =
        underlyingPersistence.clear()

    private class StoredEnvSession
    @JsonCreator
    constructor(
        @JsonProperty("a")
        val apiUrl: String,
        @JsonProperty("crt")
        val clientCertificateAlias: String?,
        @JsonProperty("i")
        val id: String,
        @JsonProperty("pt")
        val previewToken: String,
        @JsonProperty("dt")
        val downloadToken: String,
    ) {
        constructor(source: EnvSession) : this(
            id = source.id,
            apiUrl = source.envConnectionParams.apiUrl,
            clientCertificateAlias = source.envConnectionParams.clientCertificateAlias,
            previewToken = source.previewToken,
            downloadToken = source.downloadToken,
        )

        fun toSource() = EnvSession(
            id = id,
            envConnectionParams = EnvConnectionParams(
                apiUrl = apiUrl,
                clientCertificateAlias = clientCertificateAlias,
            ),
            previewToken = previewToken,
            downloadToken = downloadToken,
        )
    }
}