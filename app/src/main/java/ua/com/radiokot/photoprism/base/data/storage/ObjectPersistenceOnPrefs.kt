package ua.com.radiokot.photoprism.base.data.storage

import android.content.SharedPreferences
import androidx.core.content.edit
import ua.com.radiokot.photoprism.base.data.storage.ObjectPersistenceOnPrefs.Companion.forType
import ua.com.radiokot.photoprism.di.JsonObjectMapper

/**
 * Implements persistence for an object of type [T]
 * based on [SharedPreferences] with JSON serialization
 *
 * @see forType
 */
open class ObjectPersistenceOnPrefs<T : Any>(
    protected open val key: String,
    protected open val itemClass: Class<T>,
    protected open val preferences: SharedPreferences,
    protected open val jsonObjectMapper: JsonObjectMapper,
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
        preferences.edit {
            putString(key, serializeItem(item))
        }
    }

    override fun hasItem(): Boolean {
        return loadItem() != null
    }

    override fun clear() {
        loadedItem = null
        preferences.edit {
            remove(key)
        }
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
            jsonObjectMapper: JsonObjectMapper,
        ) =
            ObjectPersistenceOnPrefs(key, T::class.java, preferences, jsonObjectMapper)
    }
}