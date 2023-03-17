package ua.com.radiokot.photoprism.base.data.storage

import android.content.SharedPreferences
import com.fasterxml.jackson.databind.ObjectMapper
import ua.com.radiokot.photoprism.base.data.storage.ObjectPersistenceOnPrefs.Companion.forType

/**
 * Implements persistence for an object of type [T]
 * based on [SharedPreferences] with [ObjectMapper] serialization
 *
 * @see forType
 */
open class ObjectPersistenceOnPrefs<T : Any>(
    protected open val key: String,
    protected open val itemClass: Class<T>,
    protected open val preferences: SharedPreferences,
    protected open val jsonObjectMapper: ObjectMapper,
) : ObjectPersistence<T> {
    protected open var loadedItem: T? = null

    override fun loadItem(): T? {
        return loadedItem
            ?: preferences
                .getString(key, null)
                ?.let(this::deserializeItem)
                ?.also { loadedItem = it }
    }

    override fun saveItem(item: T) {
        loadedItem = item
        preferences
            .edit()
            .putString(key, serializeItem(item))
            .apply()
    }

    override fun hasItem(): Boolean {
        return loadItem() != null
    }

    override fun clear() {
        loadedItem = null
        preferences
            .edit()
            .remove(key)
            .apply()
    }

    protected open fun serializeItem(item: T): String =
        jsonObjectMapper.writeValueAsString(item)

    protected open fun deserializeItem(serialized: String): T? =
        try {
            jsonObjectMapper.readValue(serialized, itemClass)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }

    companion object {
        inline fun <reified T : Any> forType(
            key: String,
            preferences: SharedPreferences,
            jsonObjectMapper: ObjectMapper,
        ) =
            ObjectPersistenceOnPrefs(key, T::class.java, preferences, jsonObjectMapper)
    }
}