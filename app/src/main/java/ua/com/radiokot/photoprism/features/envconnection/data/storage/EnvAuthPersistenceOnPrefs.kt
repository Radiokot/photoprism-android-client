package ua.com.radiokot.photoprism.features.envconnection.data.storage

import android.content.SharedPreferences
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.JsonNode
import ua.com.radiokot.photoprism.base.data.storage.ObjectPersistence
import ua.com.radiokot.photoprism.base.data.storage.ObjectPersistenceOnPrefs
import ua.com.radiokot.photoprism.di.JsonObjectMapper
import ua.com.radiokot.photoprism.env.data.model.EnvAuth
import ua.com.radiokot.photoprism.extension.kLogger
import ua.com.radiokot.photoprism.extension.tryOrNull

class EnvAuthPersistenceOnPrefs(
    key: String,
    preferences: SharedPreferences,
    private val jsonObjectMapper: JsonObjectMapper,
) : ObjectPersistence<EnvAuth> {
    private val log = kLogger("EnvAuthPersistenceSP")

    private val v1Persistence =
        ObjectPersistenceOnPrefs.forType<StoredEnvAuth>(key, preferences, jsonObjectMapper)

    private val v2Persistence: ObjectPersistenceOnPrefs<StoredEnvAuth2> by lazy {
        migrate()
        ObjectPersistenceOnPrefs.forType<StoredEnvAuth2>("${key}_v2", preferences, jsonObjectMapper)
    }

    private fun migrate() {
        try {
            v1Persistence.loadItem()
                ?.also { storedV1 ->
                    v2Persistence.saveItem(storedV1.toV2(jsonObjectMapper))
                    v1Persistence.clear()

                    log.debug { "migrate(): migrated_from_1_to_2" }
                }
        } catch (e: Exception) {
            log.error(e) { "migrate(): migration_failed" }
        }
    }

    override fun loadItem(): EnvAuth? = tryOrNull {
        v2Persistence.loadItem()?.toSource()
    }

    override fun saveItem(item: EnvAuth) =
        v2Persistence.saveItem(StoredEnvAuth2(item))

    override fun hasItem(): Boolean =
        v2Persistence.hasItem()

    override fun clear() =
        v2Persistence.clear()

    private class StoredEnvAuth
    @JsonCreator
    constructor(
        @JsonProperty("t")
        val typeName: TypeName,

        @JsonProperty("d")
        val data: JsonNode,
    ) {
        enum class TypeName {
            CREDENTIALS,
            PUBLIC,
            ;
        }

        private sealed interface Data {
            class Credentials
            @JsonCreator
            constructor(
                @JsonProperty("u")
                val username: String,

                @JsonProperty("p")
                val password: String,
            ) : Data
        }

        fun toV2(jsonObjectMapper: JsonObjectMapper): StoredEnvAuth2 = when (typeName) {
            TypeName.CREDENTIALS ->
                jsonObjectMapper.treeToValue(data, Data.Credentials::class.java)
                    .let { v1Credentials ->
                        StoredEnvAuth2(
                            credentials = StoredEnvAuth2.Credentials(
                                username = v1Credentials.username,
                                password = v1Credentials.password,
                            ),
                            clientCertificateAlias = null,
                        )
                    }
            TypeName.PUBLIC ->
                StoredEnvAuth2(
                    credentials = null,
                    clientCertificateAlias = null,
                )
        }
    }

    private class StoredEnvAuth2
    @JsonCreator
    constructor(
        @JsonProperty("c")
        val credentials: Credentials?,
        @JsonProperty("crt")
        val clientCertificateAlias: String?,
        @JsonProperty("V")
        val version: Int = 2,
    ) {
        init {
            require(version == 2)
        }

        constructor(source: EnvAuth) : this(
            credentials = source.credentials?.let(::Credentials),
            clientCertificateAlias = source.clientCertificateAlias
        )

        fun toSource() = EnvAuth(
            credentials = credentials?.toSource(),
            clientCertificateAlias = clientCertificateAlias,
        )

        class Credentials
        @JsonCreator
        constructor(
            @JsonProperty("u")
            val username: String,

            @JsonProperty("p")
            val password: String,
        ) {
            constructor(source: EnvAuth.Credentials) : this(
                username = source.username,
                password = source.password,
            )

            fun toSource() = EnvAuth.Credentials(
                username = username,
                password = password,
            )
        }
    }
}