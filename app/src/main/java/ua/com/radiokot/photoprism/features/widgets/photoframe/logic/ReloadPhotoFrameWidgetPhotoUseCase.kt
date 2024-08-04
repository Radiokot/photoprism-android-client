package ua.com.radiokot.photoprism.features.widgets.photoframe.logic

import android.appwidget.AppWidgetManager
import android.content.Context
import android.graphics.Bitmap
import android.util.Size
import com.squareup.picasso.Picasso
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.kotlin.toCompletable
import io.reactivex.rxjava3.schedulers.Schedulers
import ua.com.radiokot.photoprism.extension.capitalized
import ua.com.radiokot.photoprism.extension.intoSingle
import ua.com.radiokot.photoprism.extension.kLogger
import ua.com.radiokot.photoprism.extension.toSingle
import ua.com.radiokot.photoprism.features.widgets.photoframe.data.model.PhotoFrameWidgetPhoto
import ua.com.radiokot.photoprism.features.widgets.photoframe.data.model.PhotoFrameWidgetShape
import ua.com.radiokot.photoprism.features.widgets.photoframe.data.storage.PhotoFrameWidgetsPreferences
import ua.com.radiokot.photoprism.features.widgets.photoframe.view.PhotoFrameWidgetRemoteViews
import java.text.DateFormat

class ReloadPhotoFrameWidgetPhotoUseCase(
    private val picasso: Picasso,
    private val widgetsPreferences: PhotoFrameWidgetsPreferences,
    private val dayYearShortDateFormat: DateFormat,
    private val context: Context,
) {
    private val log = kLogger("ReloadPhotoFrameWidgetPhotoUseCase")
    private val appWidgetManager = AppWidgetManager.getInstance(context)

    operator fun invoke(
        widgetId: Int,
    ): Completable {
        lateinit var preferences: WidgetPreferences

        return getPreferences(widgetId)
            .flatMap {
                log.debug {
                    "invoke(): got_preferences:" +
                            "\npreferences=$widgetsPreferences"
                }

                preferences = it

                getPhotoBitmap(
                    widgetSize = preferences.size,
                    shape = preferences.shape,
                    photo = preferences.photo,
                )
            }
            .flatMapCompletable { photoBitmap ->
                log.debug {
                    "getPhotoBitmap(): photo_bitmap_loaded_successfully:" +
                            "\nwidgetSize=${preferences.size}" +
                            "\nphotoPreviewUrl=${preferences.photo.previewUrl}"
                }

                showPhotoInWidget(
                    widgetId = widgetId,
                    shape = preferences.shape,
                    photo = preferences.photo,
                    photoBitmap = photoBitmap,
                    showDate = preferences.isDateShown,
                )
            }
            .doOnComplete {
                log.debug {
                    "showPhotoInWidget(): photo_shown_successfully:" +
                            "\nwidgetId=$widgetId"
                }
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

    private fun showPhotoInWidget(
        widgetId: Int,
        shape: PhotoFrameWidgetShape,
        photo: PhotoFrameWidgetPhoto,
        photoBitmap: Bitmap,
        showDate: Boolean,
    ): Completable = {
        appWidgetManager.partiallyUpdateAppWidget(
            widgetId,
            PhotoFrameWidgetRemoteViews(context) {
                hideDefaultBackground()
                setPhotoBitmap(photoBitmap)

                if (showDate) {
                    setDateVisible(true)
                    setDate(
                        date = dayYearShortDateFormat.format(photo.takenAtLocal).capitalized(),
                        gravity = shape.innerTextGravity,
                    )
                } else {
                    setDateVisible(false)
                }

                openPhotoOnClick(
                    context = context,
                    widgetId = widgetId,
                    photoUid = photo.uid,
                )
            }
        )
    }.toCompletable()


    private data class WidgetPreferences(
        val size: Size,
        val shape: PhotoFrameWidgetShape,
        val photo: PhotoFrameWidgetPhoto,
        val isDateShown: Boolean,
    )
}
