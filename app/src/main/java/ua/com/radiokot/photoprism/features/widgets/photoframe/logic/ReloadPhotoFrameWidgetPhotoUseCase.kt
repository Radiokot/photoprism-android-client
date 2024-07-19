package ua.com.radiokot.photoprism.features.widgets.photoframe.logic

import android.appwidget.AppWidgetManager
import android.content.Context
import android.widget.RemoteViews
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import ua.com.radiokot.photoprism.R
import ua.com.radiokot.photoprism.extension.kLogger
import ua.com.radiokot.photoprism.extension.tryOrNull
import ua.com.radiokot.photoprism.features.widgets.photoframe.data.storage.PhotoFrameWidgetsPreferences

class ReloadPhotoFrameWidgetPhotoUseCase(
    private val picasso: Picasso,
    private val widgetsPreferences: PhotoFrameWidgetsPreferences,
    private val context: Context,
) {
    private val log = kLogger("ReloadPhotoFrameWidgetPhotoUseCase")

    operator fun invoke(
        widgetId: Int,
    ) {
        val photoUrl = widgetsPreferences.getPhotoUrl(widgetId)
        if (photoUrl == null) {
            log.warn {
                "invoke(): photo_url_not_set:" +
                        "\nwidgetId=$widgetId"
            }
            return
        }

        val widgetSize = tryOrNull { widgetsPreferences.getSize(widgetId) }
        if (widgetSize == null) {
            log.error {
                "invoke(): size_not_set:" +
                        "\nwidgetId=$widgetId"
            }
            return
        }

        val remoteViews = RemoteViews(context.packageName, R.layout.widget_photo_frame)

        log.debug {
            "invoke(): loading:" +
                    "\nwidgetId=$widgetId," +
                    "\nphotoUrl=$photoUrl"
        }

        picasso
            .load(photoUrl)
            .resize(widgetSize.width, widgetSize.height)
            .centerCrop()
            .into(remoteViews, R.id.photo_image_view, intArrayOf(widgetId), object : Callback {
                override fun onSuccess() {
                    remoteViews.setInt(
                        R.id.photo_image_view,
                        "setBackgroundResource",
                        android.R.color.transparent
                    )

                    AppWidgetManager
                        .getInstance(context)
                        .updateAppWidget(widgetId, remoteViews)

                    log.debug {
                        "invoke(): photo_reloaded_successfully:" +
                                "\nwidgetId=$widgetId"
                    }
                }

                override fun onError(e: Exception) {
                    log.error(e) {
                        "invoke(): failed_loading_photo:" +
                                "\nphotoUrl=$photoUrl"
                    }
                }
            })
    }
}
