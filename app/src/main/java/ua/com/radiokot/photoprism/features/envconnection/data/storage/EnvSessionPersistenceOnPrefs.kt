package ua.com.radiokot.photoprism.features.envconnection.data.storage

import android.content.SharedPreferences
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import ua.com.radiokot.photoprism.base.data.storage.ObjectPersistence
import ua.com.radiokot.photoprism.base.data.storage.ObjectPersistenceOnPrefs
import ua.com.radiokot.photoprism.di.JsonObjectMapper
import ua.com.radiokot.photoprism.env.data.model.EnvConnectionParams
import ua.com.radiokot.photoprism.env.data.model.EnvSession
import ua.com.radiokot.photoprism.extension.checkNotNull
import ua.com.radiokot.photoprism.extension.kLogger
import ua.com.radiokot.photoprism.extension.tryOrNull

class EnvSessionPersistenceOnPrefs(
    key: String,
    preferences: SharedPreferences,
    jsonObjectMapper: JsonObjectMapper,
) : ObjectPersistence<EnvSession> {
    private val log = kLogger("EnvSessionPersistenceSP")

    private val v1Persistence =
        ObjectPersistenceOnPrefs.forType<StoredEnvSession>(key, preferences, jsonObjectMapper)

    private val v2Persistence =
        ObjectPersistenceOnPrefs.forType<StoredEnvSessionV2>(
            key + 2,
            preferences,
            jsonObjectMapper
        )

    private val underlyingPersistence: ObjectPersistenceOnPrefs<StoredEnvSessionV2> by lazy {
        migrate()
        v2Persistence
    }

    private fun migrate() {
        try {
            v1Persistence.loadItem()
                ?.also { storedV1 ->
                    v2Persistence.saveItem(storedV1.toV2())
                    v1Persistence.clear()

                    log.debug { "migrate(): migrated_from_1_to_2" }
                }
        } catch (e: Exception) {
            log.error(e) { "migrate(): migration_failed" }
        }
    }

    override fun loadItem(): EnvSession? = tryOrNull {
        underlyingPersistence.loadItem()?.toSource()
    }

    override fun saveItem(item: EnvSession) =
        underlyingPersistence.saveItem(StoredEnvSessionV2(item))

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
        fun toV2() = StoredEnvSessionV2(
            id = id,
            rootUrl = apiUrl.substringBeforeLast(delimiter = "api/", missingDelimiterValue = "")
                .checkNotNull {
                    "How come there is no 'api/' in the V1 apiUrl?"
                },
            clientCertificateAlias = clientCertificateAlias,
            previewToken = previewToken,
            downloadToken = downloadToken,
        )
    }

    private class StoredEnvSessionV2
    @JsonCreator
    constructor(
        @JsonProperty("r")
        val rootUrl: String,
        @JsonProperty("c")
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
            rootUrl = source.envConnectionParams.rootUrl.toString(),
            clientCertificateAlias = source.envConnectionParams.clientCertificateAlias,
            previewToken = source.previewToken,
            downloadToken = source.downloadToken,
        )

        fun toSource() = EnvSession(
            id = id,
            envConnectionParams = EnvConnectionParams(
                rootUrlString = rootUrl,
                clientCertificateAlias = clientCertificateAlias,
            ),
            previewToken = previewToken,
            downloadToken = downloadToken,
        )
    }
}