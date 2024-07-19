package ua.com.radiokot.photoprism.features.widgets.photoframe.view

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.util.Size
import org.koin.core.component.KoinScopeComponent
import org.koin.core.component.createScope
import org.koin.core.component.get
import org.koin.core.component.inject
import org.koin.core.scope.Scope
import ua.com.radiokot.photoprism.di.DI_SCOPE_SESSION
import ua.com.radiokot.photoprism.extension.kLogger
import ua.com.radiokot.photoprism.features.widgets.photoframe.data.storage.PhotoFrameWidgetsPreferences
import ua.com.radiokot.photoprism.features.widgets.photoframe.logic.ReloadPhotoFrameWidgetPhotoUseCase


class PhotoFrameWidgetProvider : AppWidgetProvider(), KoinScopeComponent {
    override val scope: Scope by lazy {
        // Prefer the session scope, but allow running without it.
        getKoin().getScopeOrNull(DI_SCOPE_SESSION) ?: createScope()
    }

    private val log = kLogger("PhotoFrameWidgetProvider")
    private val widgetsPreferences: PhotoFrameWidgetsPreferences by inject()

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) = appWidgetIds.forEach { widgetId ->
        log.debug {
            "onUpdate(): updating:" +
                    "\nwidgetId=$widgetId"
        }

        saveSizeToPreferences(
            appWidgetId = widgetId,
            widgetOptions = appWidgetManager.getAppWidgetOptions(widgetId),
            context = context,
        )
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        log.debug {
            "onAppWidgetOptionsChanged(): options_changed:" +
                    "\nwidgetId:${appWidgetId}"
        }

        saveSizeToPreferences(
            appWidgetId = appWidgetId,
            widgetOptions = newOptions,
            context = context,
        )

        try {
            get<ReloadPhotoFrameWidgetPhotoUseCase>()
                .invoke(appWidgetId)
        } catch (e: Exception) {
            log.warn {
                "onAppWidgetOptionsChanged(): failed_photo_reloading_as_missing_scope"
            }
        }
    }

    override fun onDeleted(context: Context?, appWidgetIds: IntArray) {
        log.debug {
            "onDeleted(): deleting_preferences:" +
                    "\nwidgetIds=${appWidgetIds.joinToString()}"
        }

        appWidgetIds.forEach(widgetsPreferences::delete)
    }

    private fun saveSizeToPreferences(
        appWidgetId: Int,
        widgetOptions: Bundle,
        context: Context,
    ) {
        // Given context actually corresponds to the launcher
        // and has its orientation even if doesn't match the physical one.
        val orientation = context.resources.configuration.orientation

        val width =
            if (orientation == Configuration.ORIENTATION_LANDSCAPE)
                widgetOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH)
            else
                widgetOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)

        val height =
            if (orientation == Configuration.ORIENTATION_LANDSCAPE)
                widgetOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT)
            else
                widgetOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT)

        val widthPx = (width * context.resources.displayMetrics.density).toInt()
        val heightPx = (height * context.resources.displayMetrics.density).toInt()

        log.debug {
            "saveSizeToPreferences(): saving:" +
                    "\nwidgetId=$appWidgetId," +
                    "\nwidthPx=$widthPx," +
                    "\nheightPx=$heightPx," +
                    "\norientation=$orientation"
        }

        widgetsPreferences.setSize(
            widgetId = appWidgetId,
            size = Size(widthPx, heightPx),
        )
    }
}
