package ua.com.radiokot.photoprism.features.widgets.photoframe.data.storage

import android.util.Size
import ua.com.radiokot.photoprism.features.gallery.data.model.SearchConfig

interface PhotoFrameWidgetsPreferences {
    /**
     * @return size in pixels of the given widget regarding the orientation.
     * @throws IllegalStateException if no size set
     */
    fun getSize(widgetId: Int): Size

    /**
     * Saves the size in pixels of the given widget.
     */
    fun setSize(widgetId: Int, size: Size)

    /**
     * @return URL of a photo currently shown in the given widget if it is set.
     */
    fun getPhotoUrl(widgetId: Int): String?

    /**
     * Saves URL of the photo currently shown in the given widget.
     */
    fun setPhotoUrl(widgetId: Int, photoUrl: String)

    /**
     * @return [SearchConfig] defining the set of photos to pick from for this widget
     * or a default value if it is not yet set.
     */
    fun getSearchConfig(widgetId: Int): SearchConfig

    /**
     * Saves [SearchConfig] defining the set of photos to pick from for this widget.
     */
    fun setSearchConfig(widgetId: Int, searchConfig: SearchConfig)

    /**
     * Wipes all the preferences of the given widget.
     */
    fun clear(widgetId: Int)
}
