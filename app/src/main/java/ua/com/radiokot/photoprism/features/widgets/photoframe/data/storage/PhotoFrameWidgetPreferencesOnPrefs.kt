package ua.com.radiokot.photoprism.features.widgets.photoframe.data.storage

import android.content.SharedPreferences
import android.util.Size
import androidx.collection.LruCache
import androidx.core.content.edit
import ua.com.radiokot.photoprism.base.data.storage.ObjectPersistence
import ua.com.radiokot.photoprism.di.JsonObjectMapper
import ua.com.radiokot.photoprism.extension.checkNotNull
import ua.com.radiokot.photoprism.features.gallery.data.model.SearchConfig
import ua.com.radiokot.photoprism.features.gallery.data.storage.SearchConfigPersistenceOnPrefs
import ua.com.radiokot.photoprism.features.widgets.photoframe.data.model.PhotoFrameWidgetShape

class PhotoFrameWidgetPreferencesOnPrefs(
    private val keyPrefix: String,
    private val preferences: SharedPreferences,
    private val jsonObjectMapper: JsonObjectMapper,
    private val defaultSearchConfig: SearchConfig,
    private val defaultShape: PhotoFrameWidgetShape,
) : PhotoFrameWidgetsPreferences {

    private fun getWidgetKeyPrefix(widgetId: Int) =
        "${keyPrefix}_${widgetId}"

    private fun getSizeKey(widgetId: Int) =
        getWidgetKeyPrefix(widgetId) + "_size"

    private fun getPhotoUrlKey(widgetId: Int) =
        getWidgetKeyPrefix(widgetId) + "_photo_url"

    private fun getSearchConfigKey(widgetId: Int) =
        getWidgetKeyPrefix(widgetId) + "_search_config"

    private fun getUpdatesScheduledKey(widgetId: Int) =
        getWidgetKeyPrefix(widgetId) + "_updates_scheduled"

    private fun getShapeKey(widgetId: Int) =
        getWidgetKeyPrefix(widgetId) + "_shape"

    private val searchConfigPersistenceCache =
        LruCache<Int, ObjectPersistence<SearchConfig>>(10)

    private fun getSearchConfigPersistence(widgetId: Int) =
        searchConfigPersistenceCache.get(widgetId)
            ?: SearchConfigPersistenceOnPrefs(
                key = getSearchConfigKey(widgetId),
                preferences = preferences,
                jsonObjectMapper = jsonObjectMapper,
            ).also {
                searchConfigPersistenceCache.put(widgetId, it)
            }

    override fun getSize(widgetId: Int): Size =
        preferences.getString(getSizeKey(widgetId), null)
            ?.let(Size::parseSize)
            .checkNotNull { "No size set for $widgetId" }

    override fun setSize(widgetId: Int, size: Size) =
        preferences.edit {
            putString(getSizeKey(widgetId), size.toString())
        }

    override fun getPhotoUrl(widgetId: Int): String? =
        preferences.getString(getPhotoUrlKey(widgetId), null)

    override fun setPhotoUrl(widgetId: Int, photoUrl: String) =
        preferences.edit {
            putString(getPhotoUrlKey(widgetId), photoUrl)
        }

    override fun getSearchConfig(widgetId: Int): SearchConfig =
        getSearchConfigPersistence(widgetId)
            .loadItem()
            ?: defaultSearchConfig

    override fun setSearchConfig(widgetId: Int, searchConfig: SearchConfig) =
        getSearchConfigPersistence(widgetId)
            .saveItem(searchConfig)

    override fun areUpdatesScheduled(widgetId: Int): Boolean =
        preferences.getBoolean(getUpdatesScheduledKey(widgetId), false)

    override fun setUpdatesScheduled(widgetId: Int, areScheduled: Boolean) =
        preferences.edit {
            putBoolean(getUpdatesScheduledKey(widgetId), areScheduled)
        }

    override fun getShape(widgetId: Int): PhotoFrameWidgetShape =
        preferences.getString(getShapeKey(widgetId), null)
            ?.let(PhotoFrameWidgetShape::valueOf)
            ?: defaultShape

    override fun setShape(widgetId: Int, shape: PhotoFrameWidgetShape) =
        preferences.edit{
            putString(getShapeKey(widgetId), shape.name)
        }

    override fun clear(widgetId: Int) {
        getSearchConfigPersistence(widgetId).clear()
        searchConfigPersistenceCache.remove(widgetId)
        preferences.edit {
            val widgetKeyPrefix = getWidgetKeyPrefix(widgetId)
            preferences.all.keys.forEach { key ->
                if (key.startsWith(widgetKeyPrefix)) {
                    remove(key)
                }
            }
        }
    }
}
