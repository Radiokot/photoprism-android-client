package ua.com.radiokot.photoprism.features.widgets.photoframe.view

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.view.View
import android.widget.RemoteViews
import androidx.annotation.GravityInt
import ua.com.radiokot.photoprism.R
import ua.com.radiokot.photoprism.features.gallery.data.model.SearchConfig
import ua.com.radiokot.photoprism.features.gallery.data.storage.SimpleGalleryMediaRepository
import ua.com.radiokot.photoprism.features.viewer.view.MediaViewerActivity

class PhotoFrameWidgetRemoteViews(
    context: Context,
    update: PhotoFrameWidgetRemoteViews.() -> Unit = {},
) : RemoteViews(context.packageName, R.layout.widget_photo_frame) {
    init {
        apply(update)
    }

    fun setLoadingVisible(isVisible: Boolean) =
        setViewVisibility(
            R.id.progress_indicator,
            if (isVisible)
                View.VISIBLE
            else
                View.GONE
        )

    fun hideDefaultBackground() =
        setInt(
            R.id.photo_image_view,
            "setBackgroundResource",
            android.R.color.transparent
        )

    fun setPhotoBitmap(photoBitmap: Bitmap) =
        setImageViewBitmap(
            R.id.photo_image_view,
            photoBitmap
        )

    fun setDateVisible(isVisible: Boolean) =
        setViewVisibility(
            R.id.date_layout,
            if (isVisible)
                View.VISIBLE
            else
                View.GONE
        )

    fun setDate(
        date: String,
        @GravityInt
        gravity: Int,
    ) {
        setInt(
            R.id.date_layout,
            "setGravity",
            gravity
        )
        setTextViewText(
            R.id.date_text_view,
            date
        )
    }

    fun openPhotoOnClick(
        context: Context,
        widgetId: Int,
        photoUid: String,
    ) =
        setOnClickPendingIntent(
            R.id.photo_image_view,
            PendingIntent.getActivity(
                context,
                // Widget ID is set as the intent request code
                // in order for intents for different widgets to co-exist.
                // Otherwise, if the intents only differ in extras,
                // the latest replaces all the existing.
                widgetId,
                Intent(context, MediaViewerActivity::class.java)
                    .putExtras(
                        MediaViewerActivity.getBundle(
                            mediaIndex = 0,
                            repositoryParams = SimpleGalleryMediaRepository.Params(
                                SearchConfig.DEFAULT.copy(
                                    userQuery = "uid:${photoUid}",
                                    includePrivate = true,
                                )
                            ),
                        )
                    )
                    .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
        )
}
