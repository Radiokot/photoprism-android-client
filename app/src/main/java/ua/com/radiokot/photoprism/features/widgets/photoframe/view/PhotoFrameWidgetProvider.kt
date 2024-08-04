package ua.com.radiokot.photoprism.features.widgets.photoframe.view

import android.annotation.SuppressLint
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.util.Size
import android.widget.RemoteViews
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import io.reactivex.rxjava3.kotlin.blockingSubscribeBy
import io.reactivex.rxjava3.kotlin.subscribeBy
import org.koin.core.component.KoinScopeComponent
import org.koin.core.component.createScope
import org.koin.core.component.inject
import org.koin.core.scope.Scope
import ua.com.radiokot.photoprism.R
import ua.com.radiokot.photoprism.di.DI_SCOPE_SESSION
import ua.com.radiokot.photoprism.extension.kLogger
import ua.com.radiokot.photoprism.features.widgets.photoframe.data.storage.PhotoFrameWidgetsPreferences
import ua.com.radiokot.photoprism.features.widgets.photoframe.logic.ReloadPhotoFrameWidgetPhotoUseCase
import ua.com.radiokot.photoprism.features.widgets.photoframe.logic.UpdatePhotoFrameWidgetWorker
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread


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
            widgetId = widgetId,
            widgetOptions = appWidgetManager.getAppWidgetOptions(widgetId),
            context = context,
        )

        if (!widgetsPreferences.areUpdatesScheduled(widgetId)) {
            // Should be called before WorkManager to avoid infinite loop.
            // See https://stackoverflow.com/questions/70654474/starting-workmanager-task-from-appwidgetprovider-results-in-endless-onupdate-cal
            widgetsPreferences.setUpdatesScheduled(widgetId)

            // Make the first full update to allow further partial updates.
            appWidgetManager.updateAppWidget(
                widgetId,
                RemoteViews(context.packageName, R.layout.widget_photo_frame)
            )

            scheduleWidgetUpdates(widgetId, context)
        } else {
            reloadWidgetPhotoIfPossible(widgetId)
        }
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
            widgetId = appWidgetId,
            widgetOptions = newOptions,
            context = context,
        )

        reloadWidgetPhotoIfPossible(appWidgetId)
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        appWidgetIds.forEach { widgetId ->
            widgetsPreferences.clear(widgetId)

            log.debug {
                "onDeleted(): cleared_preferences:" +
                        "\nwidgetId=$widgetId"
            }

            cancelWidgetUpdates(widgetId, context)
        }
    }

    private fun saveSizeToPreferences(
        widgetId: Int,
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
                    "\nwidgetId=$widgetId," +
                    "\nwidthPx=$widthPx," +
                    "\nheightPx=$heightPx," +
                    "\norientation=$orientation"
        }

        widgetsPreferences.setSize(
            widgetId = widgetId,
            size = Size(widthPx, heightPx),
        )
    }

    private fun scheduleWidgetUpdates(
        widgetId: Int,
        context: Context,
    ) {
        val workRequest = PeriodicWorkRequestBuilder<UpdatePhotoFrameWidgetWorker>(
            UPDATE_INTERVAL_MINUTES,
            TimeUnit.MINUTES
        )
            .setInputData(
                UpdatePhotoFrameWidgetWorker.getInputData(
                    widgetId = widgetId,
                )
            )
            .setConstraints(
                Constraints.Builder()
                    .setRequiresBatteryNotLow(true)
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .addTag(UpdatePhotoFrameWidgetWorker.TAG)
            .build()

        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(
                getWidgetUpdateWorkName(widgetId),
                ExistingPeriodicWorkPolicy.UPDATE,
                workRequest,
            )

        log.debug {
            "scheduleWidgetUpdates: scheduled:" +
                    "\nwidgetId=$widgetId," +
                    "\nintervalMinutes=$UPDATE_INTERVAL_MINUTES"
        }
    }

    private fun cancelWidgetUpdates(
        widgetId: Int,
        context: Context,
    ) {
        WorkManager.getInstance(context)
            .cancelUniqueWork(getWidgetUpdateWorkName(widgetId))

        log.debug {
            "cancelWidgetUpdates(): canceled:" +
                    "\nwidgetId=$widgetId"
        }
    }

    @SuppressLint("CheckResult")
    private fun reloadWidgetPhotoIfPossible(widgetId: Int) {
        val reloadPhotoUseCase = scope.getOrNull<ReloadPhotoFrameWidgetPhotoUseCase>()
        if (reloadPhotoUseCase == null) {
            log.warn {
                "reloadWidgetPhotoIfPossible(): failed_photo_reloading_as_missing_scope"
            }

            return
        }

        // Run it in a thread so it is not garbage collected.
        thread {
            reloadPhotoUseCase
                .invoke(widgetId)
                .blockingSubscribeBy(
                    onError = { error ->
                        log.error(error) {
                            "reloadWidgetPhotoIfPossible(): failed_photo_reloading"
                        }
                    }
                )
        }
    }

    private fun getWidgetUpdateWorkName(widgetId: Int) =
        "${UpdatePhotoFrameWidgetWorker.TAG}:$widgetId"

    private companion object {
        private const val UPDATE_INTERVAL_MINUTES = 150L
    }
}
