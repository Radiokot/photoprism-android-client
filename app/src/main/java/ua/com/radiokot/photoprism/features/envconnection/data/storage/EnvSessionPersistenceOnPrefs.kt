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
import ua.com.radiokot.photoprism.extension.basicAuth
import ua.com.radiokot.photoprism.extension.checkNotNull
import ua.com.radiokot.photoprism.extension.kLogger
import ua.com.radiokot.photoprism.extension.tryOrNull
import ua.com.radiokot.photoprism.extension.withMaskedCredentials

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

    private val v3Persistence =
        ObjectPersistenceOnPrefs.forType<StoredEnvSessionV3>(
            key + 3,
            preferences,
            jsonObjectMapper
        )

    private val underlyingPersistence: ObjectPersistenceOnPrefs<StoredEnvSessionV3> by lazy {
        migrate()
        v3Persistence
    }

    private fun migrate() {
        try {
            v1Persistence.loadItem()
                ?.also { storedV1 ->
                    v2Persistence.saveItem(storedV1.toV2())
                    v1Persistence.clear()

                    log.debug { "migrate(): migrated_from_1_to_2" }
                }
            v2Persistence.loadItem()
                ?.also { storedV2 ->
                    v3Persistence.saveItem(storedV2.toV3())
                    v2Persistence.clear()

                    log.debug { "migrate(): migrated_from_2_to_3" }
                }
        } catch (e: Exception) {
            log.error(e) { "migrate(): migration_failed" }
        }
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
        fun toV3(): StoredEnvSessionV3 {
            val rootHttpUrl = rootUrl.toHttpUrl()

            return StoredEnvSessionV3(
                id = id,
                rootUrl = rootHttpUrl.withMaskedCredentials().toString(),
                clientCertificateAlias = clientCertificateAlias,
                httpAuth = rootHttpUrl.basicAuth,
                previewToken = previewToken,
                downloadToken = downloadToken,
            )
        }
    }

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
