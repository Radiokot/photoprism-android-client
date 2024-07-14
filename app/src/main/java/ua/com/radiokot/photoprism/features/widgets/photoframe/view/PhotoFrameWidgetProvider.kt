package ua.com.radiokot.photoprism.features.widgets.photoframe.view

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.widget.RemoteViews
import com.squareup.picasso.Picasso
import ua.com.radiokot.photoprism.R
import ua.com.radiokot.photoprism.extension.kLogger

class PhotoFrameWidgetProvider : AppWidgetProvider() {
    private val log = kLogger("PhotoFrameWidgetProvider")

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) = appWidgetIds.forEach { widgetId ->
        val view = RemoteViews(context.packageName, R.layout.widget_photo_frame)

        log.debug {
            "onUpdate(): updating:" +
                    "\nwidgetId=$widgetId"
        }

        Picasso.get()
            .load("https://dl.photoprism.app/img/gophers/awake.png")
            .into(view, R.id.photo_image_view, appWidgetIds)
    }
}
