package ua.com.radiokot.photoprism.features.envconnection.data.storage

import android.content.SharedPreferences
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import okhttp3.HttpUrl.Companion.toHttpUrl
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

    private val v3Persistence =
        ObjectPersistenceOnPrefs.forType<StoredEnvSessionV3>(
            key + 3,
            preferences,
            jsonObjectMapper
        )

    private val underlyingPersistence: ObjectPersistenceOnPrefs<StoredEnvSessionV3> by lazy {
        // Add migration here in the future.
        v3Persistence
    }

    override fun loadItem(): EnvSession? = tryOrNull {
        underlyingPersistence.loadItem()?.toSource()
    }

    override fun saveItem(item: EnvSession) =
        underlyingPersistence.saveItem(StoredEnvSessionV3(item))

    override fun hasItem(): Boolean =
        underlyingPersistence.hasItem()

    override fun clear() =
        underlyingPersistence.clear()

    private class StoredEnvSessionV3
    @JsonCreator
    constructor(
        @JsonProperty("r")
        val rootUrl: String,
        @JsonProperty("c")
        val clientCertificateAlias: String?,
        @JsonProperty("a")
        val httpAuth: String?,
        @JsonProperty("i")
        val id: String,
        @JsonProperty("pt")
        val previewToken: String,
        @JsonProperty("dt")
        val downloadToken: String,
    ) {
        constructor(source: EnvSession) : this(
            id = source.id,
            rootUrl = source.envConnectionParams.rootUrl.toString(),
            clientCertificateAlias = source.envConnectionParams.clientCertificateAlias,
            httpAuth = source.envConnectionParams.httpAuth,
            previewToken = source.previewToken,
            downloadToken = source.downloadToken,
        )

        fun toSource() = EnvSession(
            id = id,
            envConnectionParams = EnvConnectionParams(
                rootUrl = rootUrl.toHttpUrl(),
                clientCertificateAlias = clientCertificateAlias,
                httpAuth = httpAuth,
            ),
            previewToken = previewToken,
            downloadToken = downloadToken,
        )
    }
}
