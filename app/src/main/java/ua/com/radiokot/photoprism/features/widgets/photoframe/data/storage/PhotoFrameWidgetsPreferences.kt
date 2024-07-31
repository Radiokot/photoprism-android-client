package ua.com.radiokot.photoprism.features.widgets.photoframe.data.storage

import android.util.Size
import ua.com.radiokot.photoprism.features.gallery.data.model.SearchConfig
import ua.com.radiokot.photoprism.features.widgets.photoframe.data.model.PhotoFrameWidgetPhoto
import ua.com.radiokot.photoprism.features.widgets.photoframe.data.model.PhotoFrameWidgetShape

interface PhotoFrameWidgetsPreferences {
    /**
     * @return size in pixels of this widget regarding the orientation.
     * @throws IllegalStateException if no size set
     */
    fun getSize(widgetId: Int): Size

    /**
     * Saves the size in pixels of this widget widget.
     */
    fun setSize(widgetId: Int, size: Size)

    /**
     * @return the photo currently shown in this widget if it is set.
     */
    fun getPhoto(widgetId: Int): PhotoFrameWidgetPhoto?

    /**
     * Saves the photo to be shown in this widget.
     */
    fun setPhoto(widgetId: Int, photo: PhotoFrameWidgetPhoto)

    /**
     * @return [SearchConfig] defining the set of photos to pick from for this widget
     * if it is set.
     */
    fun getSearchConfig(widgetId: Int): SearchConfig?

    /**
     * Saves [SearchConfig] defining the set of photos to pick from for this widget.
     */
    fun setSearchConfig(widgetId: Int, searchConfig: SearchConfig?)

    /**
     * @return whether the update work is scheduled for this widget.
     */
    fun areUpdatesScheduled(widgetId: Int): Boolean

    /**
     * Saves whether the update work is scheduled for this widget.
     */
    fun setUpdatesScheduled(widgetId: Int, areScheduled: Boolean = true)

    /**
     * @return shape of this widget
     * or a default value if it is not yet set.
     */
    fun getShape(widgetId: Int): PhotoFrameWidgetShape

    /**
     * Saves shape if this widget.
     */
    fun setShape(widgetId: Int, shape: PhotoFrameWidgetShape)

    /**
     * Wipes all the preferences of this widget.
     */
    fun clear(widgetId: Int)
}
