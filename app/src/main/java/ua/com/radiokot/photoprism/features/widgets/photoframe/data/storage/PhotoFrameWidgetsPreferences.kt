package ua.com.radiokot.photoprism.features.widgets.photoframe.data.storage

import android.util.Size

interface PhotoFrameWidgetsPreferences {
    /**
     * @return size of the given widget regarding the orientation.
     */
    fun getSize(widgetId: Int): Size

    /**
     * Saves the size of the given widget.
     */
    fun setSize(widgetId: Int, size: Size)

    /**
     * @return photo URL of the given widget if set.
     */
    fun getPhotoUrl(widgetId: Int): String?

    /**
     * Saves the photo URL of the given widget.
     */
    fun setPhotoUrl(widgetId: Int, photoUrl: String)

    /**
     * Deletes all the preferences of the given widget.
     */
    fun delete(widgetId: Int)
}
