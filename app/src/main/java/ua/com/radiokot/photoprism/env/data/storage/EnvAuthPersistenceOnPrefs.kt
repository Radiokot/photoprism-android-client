package ua.com.radiokot.photoprism.env.data.storage

import android.content.SharedPreferences
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.JsonNode
import ua.com.radiokot.photoprism.base.data.storage.ObjectPersistenceOnPrefs
import ua.com.radiokot.photoprism.di.JsonObjectMapper
import ua.com.radiokot.photoprism.env.data.model.EnvAuth

class EnvAuthPersistenceOnPrefs(
    key: String,
    preferences: SharedPreferences,
    jsonObjectMapper: JsonObjectMapper,
) : ObjectPersistenceOnPrefs<EnvAuth>(
    key = key,
    preferences = preferences,
    jsonObjectMapper = jsonObjectMapper,
    itemClass = EnvAuth::class.java
) {
    private class Bucket(
        @JsonProperty("type")
        @get:JsonProperty("type")
        val type: TypeName,

        @JsonProperty("data")
        @get:JsonProperty("data")
        val data: JsonNode,
    ) {
        enum class TypeName {
            CREDENTIALS,
            PUBLIC,
            ;
        }
    }

    override fun serializeItem(item: EnvAuth): String {
        val data = jsonObjectMapper.valueToTree<JsonNode>(item)
        val typeName: Bucket.TypeName = when (item) {
            is EnvAuth.Credentials ->
                Bucket.TypeName.CREDENTIALS
            EnvAuth.Public ->
                Bucket.TypeName.PUBLIC
        }

        return jsonObjectMapper.writeValueAsString(Bucket(typeName, data))
    }

    override fun deserializeItem(serialized: String): EnvAuth? = try {
        val bucket: Bucket = jsonObjectMapper.readValue(serialized, Bucket::class.java)

        when (bucket.type) {
            Bucket.TypeName.CREDENTIALS ->
                jsonObjectMapper.treeToValue(bucket.data, EnvAuth.Credentials::class.java)
            Bucket.TypeName.PUBLIC ->
                EnvAuth.Public
        }
    } catch (e: Exception) {
        // Do not print logs as they may contain sensitive data.
        null
    }
}