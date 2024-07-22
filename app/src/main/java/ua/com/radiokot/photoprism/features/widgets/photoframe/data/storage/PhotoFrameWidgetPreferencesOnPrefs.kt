package ua.com.radiokot.photoprism.features.widgets.photoframe.data.storage

import android.content.SharedPreferences
import android.util.Size
import androidx.core.content.edit
import ua.com.radiokot.photoprism.extension.checkNotNull

class PhotoFrameWidgetPreferencesOnPrefs(
    private val preferences: SharedPreferences,
    private val keyPrefix: String,
) : PhotoFrameWidgetsPreferences {

    private fun getSizeKey(widgetId: Int) =
        "${keyPrefix}_size_$widgetId"

    private fun getPhotoUrlKey(widgetId: Int) =
        "${keyPrefix}_photo_url_$widgetId"

    override fun getSize(widgetId: Int): Size =
        preferences.getString(getSizeKey(widgetId), null)
            ?.let(Size::parseSize)
            .checkNotNull { "No size set for $widgetId" }

    override fun setSize(widgetId: Int, size: Size) =
        preferences.edit {
            putString(getSizeKey(widgetId), size.toString())
        }

    override fun getPhotoUrl(widgetId: Int): String =
        preferences.getString(getPhotoUrlKey(widgetId), null)
            .checkNotNull { "No photo URL set for $widgetId" }

    override fun setPhotoUrl(widgetId: Int, photoUrl: String) =
        preferences.edit {
            putString(getPhotoUrlKey(widgetId), photoUrl)
        }

    override fun delete(widgetId: Int) =
        preferences.edit {
            remove(getSizeKey(widgetId))
            remove(getPhotoUrlKey(widgetId))
        }
}
