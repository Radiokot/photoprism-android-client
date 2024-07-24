package ua.com.radiokot.photoprism.features.widgets.photoframe.logic

import android.appwidget.AppWidgetManager
import android.content.Context
import android.graphics.Bitmap
import android.util.Size
import android.widget.RemoteViews
import com.squareup.picasso.Picasso
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.kotlin.toCompletable
import ua.com.radiokot.photoprism.R
import ua.com.radiokot.photoprism.extension.checkNotNull
import ua.com.radiokot.photoprism.extension.intoSingle
import ua.com.radiokot.photoprism.extension.kLogger
import ua.com.radiokot.photoprism.extension.toSingle
import ua.com.radiokot.photoprism.features.widgets.photoframe.data.storage.PhotoFrameWidgetsPreferences

class ReloadPhotoFrameWidgetPhotoUseCase(
    private val picasso: Picasso,
    private val widgetsPreferences: PhotoFrameWidgetsPreferences,
    private val context: Context,
) {
    private val log = kLogger("ReloadPhotoFrameWidgetPhotoUseCase")

    operator fun invoke(
        widgetId: Int,
    ): Completable =
        getSizeAndPhotoUrl(widgetId)
            .flatMap { (widgetSize, photoUrl) ->
                getPhoto(widgetSize, photoUrl)
            }
            .flatMapCompletable { photoBitmap ->
                showPhotoInWidget(widgetId, photoBitmap)
            }

    private fun getSizeAndPhotoUrl(widgetId: Int): Single<Pair<Size, String>> = {
        Pair(
            first = widgetsPreferences.getSize(widgetId),
            second = widgetsPreferences.getPhotoUrl(widgetId)
                .checkNotNull {
                    "No photo URL for $widgetId yet"
                }
        )
    }.toSingle()

    private fun getPhoto(
        widgetSize: Size,
        photoUrl: String,
    ): Single<Bitmap> =
        picasso
            .load(photoUrl)
            .resize(widgetSize.width, widgetSize.height)
            .centerCrop()
            .intoSingle()
            .doOnSuccess {
                log.debug {
                    "getPhoto(): photo_loaded_successfully:" +
                            "\nwidgetSize=$widgetSize" +
                            "\nphotoUrl=$photoUrl"
                }
            }

    private fun showPhotoInWidget(
        widgetId: Int,
        photoBitmap: Bitmap,
    ): Completable = {
        val remoteViews =
            RemoteViews(context.packageName, R.layout.widget_photo_frame).apply {
                setInt(
                    R.id.photo_image_view,
                    "setBackgroundResource",
                    android.R.color.transparent
                )
                setImageViewBitmap(R.id.photo_image_view, photoBitmap)
            }

        AppWidgetManager
            .getInstance(context)
            .partiallyUpdateAppWidget(widgetId, remoteViews)

        log.debug {
            "showPhotoInWidget(): photo_shown_successfully:" +
                    "\nwidgetId=$widgetId"
        }
    }.toCompletable()
}
