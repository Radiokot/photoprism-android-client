package ua.com.radiokot.photoprism.features.envconnection.data.storage

import android.content.SharedPreferences
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.JsonNode
import ua.com.radiokot.photoprism.base.data.storage.ObjectPersistence
import ua.com.radiokot.photoprism.base.data.storage.ObjectPersistenceOnPrefs
import ua.com.radiokot.photoprism.di.JsonObjectMapper
import ua.com.radiokot.photoprism.env.data.model.EnvAuth
import ua.com.radiokot.photoprism.extension.tryOrNull

class EnvAuthPersistenceOnPrefs(
    key: String,
    preferences: SharedPreferences,
    private val jsonObjectMapper: JsonObjectMapper,
) : ObjectPersistence<EnvAuth> {
    private val underlyingPersistence =
        ObjectPersistenceOnPrefs.forType<StoredEnvAuth>(key, preferences, jsonObjectMapper)

    override fun loadItem(): EnvAuth? = tryOrNull {
        underlyingPersistence.loadItem()?.toSource(jsonObjectMapper)
    }

    override fun saveItem(item: EnvAuth) =
        underlyingPersistence.saveItem(StoredEnvAuth(item, jsonObjectMapper))

    override fun hasItem(): Boolean =
        underlyingPersistence.hasItem()

    override fun clear() =
        underlyingPersistence.clear()

    class StoredEnvAuth
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
            ) : Data {
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

        constructor(
            source: EnvAuth,
            jsonObjectMapper: JsonObjectMapper,
        ) : this(
            typeName = when (source) {
                is EnvAuth.Credentials ->
                    TypeName.CREDENTIALS
                EnvAuth.Public ->
                    TypeName.PUBLIC
            },
            data = when (source) {
                is EnvAuth.Credentials ->
                    jsonObjectMapper.valueToTree(Data.Credentials(source))
                EnvAuth.Public ->
                    jsonObjectMapper.createObjectNode()
            }
        )

        fun toSource(jsonObjectMapper: JsonObjectMapper): EnvAuth = when (typeName) {
            TypeName.CREDENTIALS ->
                jsonObjectMapper.treeToValue(data, Data.Credentials::class.java)
                    .toSource()
            TypeName.PUBLIC ->
                EnvAuth.Public
        }
    }
}