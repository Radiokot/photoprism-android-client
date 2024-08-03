package ua.com.radiokot.photoprism.features.widgets.photoframe.logic

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.util.Size
import android.view.Gravity
import android.view.View
import android.widget.RemoteViews
import com.squareup.picasso.Picasso
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.kotlin.toCompletable
import io.reactivex.rxjava3.schedulers.Schedulers
import ua.com.radiokot.photoprism.R
import ua.com.radiokot.photoprism.extension.capitalized
import ua.com.radiokot.photoprism.extension.intoSingle
import ua.com.radiokot.photoprism.extension.kLogger
import ua.com.radiokot.photoprism.extension.toSingle
import ua.com.radiokot.photoprism.features.gallery.data.model.SearchConfig
import ua.com.radiokot.photoprism.features.gallery.data.storage.SimpleGalleryMediaRepository
import ua.com.radiokot.photoprism.features.viewer.view.MediaViewerActivity
import ua.com.radiokot.photoprism.features.widgets.photoframe.data.model.PhotoFrameWidgetPhoto
import ua.com.radiokot.photoprism.features.widgets.photoframe.data.model.PhotoFrameWidgetShape
import ua.com.radiokot.photoprism.features.widgets.photoframe.data.storage.PhotoFrameWidgetsPreferences
import java.text.DateFormat

class ReloadPhotoFrameWidgetPhotoUseCase(
    private val picasso: Picasso,
    private val widgetsPreferences: PhotoFrameWidgetsPreferences,
    private val dayYearShortDateFormat: DateFormat,
    private val context: Context,
) {
    private val log = kLogger("ReloadPhotoFrameWidgetPhotoUseCase")

    operator fun invoke(
        widgetId: Int,
    ): Completable {
        lateinit var preferences: WidgetPreferences

        return getPreferences(widgetId)
            .flatMap {
                preferences = it
                getPhotoBitmap(
                    widgetSize = preferences.size,
                    shape = preferences.shape,
                    photo = preferences.photo,
                )
            }
            .flatMapCompletable { photoBitmap ->
                showPhotoInWidget(
                    widgetId = widgetId,
                    shape = preferences.shape,
                    photo = preferences.photo,
                    photoBitmap = photoBitmap,
                    showDate = preferences.isDateShown,
                )
            }
    }

    private fun getPreferences(
        widgetId: Int,
    ): Single<WidgetPreferences> = {
        WidgetPreferences(
            size = widgetsPreferences.getSize(widgetId),
            shape = widgetsPreferences.getShape(widgetId),
            photo = widgetsPreferences.getPhoto(widgetId)
                ?: error("No photo for $widgetId yet"),
            isDateShown = widgetsPreferences.isDateShown(widgetId),
        )
    }.toSingle().subscribeOn(Schedulers.io())

    private fun getPhotoBitmap(
        widgetSize: Size,
        shape: PhotoFrameWidgetShape,
        photo: PhotoFrameWidgetPhoto,
    ): Single<Bitmap> =
        picasso
            .load(photo.previewUrl)
            .resize(widgetSize.width, widgetSize.height)
            .centerCrop()
            .transform(shape.getTransformation(context))
            .intoSingle()
            .doOnSuccess {
                log.debug {
                    "getPhotoBitmap(): photo_bitmap_loaded_successfully:" +
                            "\nwidgetSize=$widgetSize" +
                            "\nphotoPreviewUrl=${photo.previewUrl}"
                }
            }

    private fun showPhotoInWidget(
        widgetId: Int,
        shape: PhotoFrameWidgetShape,
        photo: PhotoFrameWidgetPhoto,
        photoBitmap: Bitmap,
        showDate: Boolean,
    ): Completable = {
        AppWidgetManager
            .getInstance(context)
            .partiallyUpdateAppWidget(
                widgetId,
                RemoteViews(context.packageName, R.layout.widget_photo_frame).apply {
                    setViewVisibility(R.id.progress_bar, View.GONE)

                    setInt(
                        R.id.photo_image_view,
                        "setBackgroundResource",
                        android.R.color.transparent
                    )
                    setImageViewBitmap(R.id.photo_image_view, photoBitmap)

                    if (showDate) {
                        setViewVisibility(R.id.date_layout, View.VISIBLE)
                        setInt(
                            R.id.date_layout,
                            "setGravity",
                            shape.innerTextGravity
                        )

                        setTextViewText(
                            R.id.date_text_view,
                            dayYearShortDateFormat.format(photo.takenAtLocal).capitalized()
                        )
                    } else {
                        setViewVisibility(R.id.date_layout, View.GONE)
                    }

                    setOnClickPendingIntent(
                        R.id.photo_image_view,
                        getOpenPhotoPendingIntent(photo)
                    )
                }
            )

        log.debug {
            "showPhotoInWidget(): photo_shown_successfully:" +
                    "\nwidgetId=$widgetId"
        }
    }.toCompletable()

    private fun getOpenPhotoPendingIntent(photo: PhotoFrameWidgetPhoto) =
        PendingIntent.getActivity(
            context,
            0,
            Intent(context, MediaViewerActivity::class.java)
                .putExtras(
                    MediaViewerActivity.getBundle(
                        mediaIndex = 0,
                        repositoryParams = SimpleGalleryMediaRepository.Params(
                            SearchConfig.DEFAULT.copy(
                                userQuery = "uid:${photo.uid}",
                                includePrivate = true,
                            )
                        ),
                    )
                )
                .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

    private data class WidgetPreferences(
        val size: Size,
        val shape: PhotoFrameWidgetShape,
        val photo: PhotoFrameWidgetPhoto,
        val isDateShown: Boolean,
    )
}
