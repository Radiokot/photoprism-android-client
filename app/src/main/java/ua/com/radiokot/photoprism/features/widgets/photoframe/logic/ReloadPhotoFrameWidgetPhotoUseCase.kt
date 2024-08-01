package ua.com.radiokot.photoprism.features.widgets.photoframe.logic

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.util.Size
import android.view.View
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
import ua.com.radiokot.photoprism.features.gallery.data.model.SearchConfig
import ua.com.radiokot.photoprism.features.gallery.data.storage.SimpleGalleryMediaRepository
import ua.com.radiokot.photoprism.features.viewer.view.MediaViewerActivity
import ua.com.radiokot.photoprism.features.widgets.photoframe.data.model.PhotoFrameWidgetPhoto
import ua.com.radiokot.photoprism.features.widgets.photoframe.data.model.PhotoFrameWidgetShape
import ua.com.radiokot.photoprism.features.widgets.photoframe.data.storage.PhotoFrameWidgetsPreferences

class ReloadPhotoFrameWidgetPhotoUseCase(
    private val picasso: Picasso,
    private val widgetsPreferences: PhotoFrameWidgetsPreferences,
    private val context: Context,
) {
    private val log = kLogger("ReloadPhotoFrameWidgetPhotoUseCase")

    operator fun invoke(
        widgetId: Int,
    ): Completable {
        lateinit var photo: PhotoFrameWidgetPhoto

        return getPreferences(widgetId)
            .flatMap { (size, shape, widgetPhoto) ->
                photo = widgetPhoto
                getPhotoBitmap(size, shape, photo)
            }
            .flatMapCompletable { photoBitmap ->
                showPhotoInWidget(widgetId, photo, photoBitmap)
            }
    }

    private fun getPreferences(widgetId: Int): Single<Triple<Size, PhotoFrameWidgetShape, PhotoFrameWidgetPhoto>> =
        {
            Triple(
                first = widgetsPreferences.getSize(widgetId),
                second = widgetsPreferences.getShape(widgetId),
                third = widgetsPreferences.getPhoto(widgetId)
                    .checkNotNull {
                        "No photo for $widgetId yet"
                    }
            )
        }.toSingle()

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
        photo: PhotoFrameWidgetPhoto,
        photoBitmap: Bitmap,
    ): Completable = {
        AppWidgetManager
            .getInstance(context)
            .partiallyUpdateAppWidget(
                widgetId,
                RemoteViews(context.packageName, R.layout.widget_photo_frame).apply {
                    setInt(
                        R.id.photo_image_view,
                        "setBackgroundResource",
                        android.R.color.transparent
                    )
                    setImageViewBitmap(R.id.photo_image_view, photoBitmap)
                    setViewVisibility(R.id.progress_bar, View.GONE)

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
}
