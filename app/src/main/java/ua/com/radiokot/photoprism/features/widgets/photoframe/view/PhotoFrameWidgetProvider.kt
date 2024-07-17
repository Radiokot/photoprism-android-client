package ua.com.radiokot.photoprism.features.widgets.photoframe.view

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import ua.com.radiokot.photoprism.extension.kLogger
import kotlin.math.ceil


class PhotoFrameWidgetProvider : AppWidgetProvider() {
    private val log = kLogger("PhotoFrameWidgetProvider")

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) = appWidgetIds.forEach { widgetId ->
        log.debug {
            "onUpdate(): updating:" +
                    "\nwidgetId=$widgetId"
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        val portraitWidth = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)
        val landsWidth = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH)
        val landsHeight = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT)
        val portraitHeight = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT)
        val orientation = context.resources.configuration.orientation

        val width =
            if (orientation == Configuration.ORIENTATION_LANDSCAPE)
                landsWidth
            else
                portraitWidth

        val height =
            if (orientation == Configuration.ORIENTATION_LANDSCAPE)
                landsHeight
            else
                portraitHeight

        val cellWidth = ceil(width.toFloat() / 50)
        val cellHeight = ceil(height.toFloat() / 30)

        val pxWidth = width * context.resources.displayMetrics.density
        val pxHeight = height * context.resources.displayMetrics.density

        log.debug {
            "onAppWidgetOptionsChanged(): options_changed:" +
                    "\ncellWidth=$cellWidth," +
                    "\ncellHeight=$cellHeight," +
                    "\npxWidth=$pxWidth," +
                    "\npxHeight=$pxHeight"
        }
    }
}
