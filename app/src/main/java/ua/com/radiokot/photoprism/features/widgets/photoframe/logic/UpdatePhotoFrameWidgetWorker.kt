package ua.com.radiokot.photoprism.features.widgets.photoframe.logic

import android.appwidget.AppWidgetManager
import android.content.Context
import androidx.work.Data
import androidx.work.WorkerParameters
import androidx.work.rxjava3.RxWorker
import io.reactivex.rxjava3.core.Single
import org.koin.core.component.KoinScopeComponent
import org.koin.core.component.createScope
import org.koin.core.component.inject
import org.koin.core.scope.Scope
import ua.com.radiokot.photoprism.di.DI_SCOPE_SESSION
import ua.com.radiokot.photoprism.extension.kLogger
import ua.com.radiokot.photoprism.features.widgets.photoframe.view.PhotoFrameWidgetRemoteViews

class UpdatePhotoFrameWidgetWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : RxWorker(appContext, workerParams),
    KoinScopeComponent {
    override val scope: Scope by lazy {
        // Prefer the session scope, but allow running without it.
        getKoin().getScopeOrNull(DI_SCOPE_SESSION) ?: createScope()
    }

    private val log = kLogger("UpdatePhotoFrameWidgetWorker")
    private val appWidgetManager = AppWidgetManager.getInstance(appContext)
    private val updatePhotoFrameWidgetPhotoUseCase: UpdatePhotoFrameWidgetPhotoUseCase by inject()
    private val reloadPhotoFrameWidgetPhotoUseCase: ReloadPhotoFrameWidgetPhotoUseCase by inject()
    private val widgetId: Int by lazy {
        workerParams.inputData.getInt(
            WIDGET_ID_KEY,
            AppWidgetManager.INVALID_APPWIDGET_ID
        )
    }

    override fun createWork(): Single<Result> {
        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            log.warn {
                "createWork(): skip_as_missing_widget_id"
            }

            return Single.just(Result.success())
        }

        if (scope.id != DI_SCOPE_SESSION) {
            log.debug {
                "createWork(): skip_as_missing_session_scope"
            }

            return Single.just(Result.success())
        }

        return updatePhotoFrameWidgetPhotoUseCase
            .invoke(widgetId)
            .andThen(reloadPhotoFrameWidgetPhotoUseCase(widgetId))
            .toSingleDefault(Result.success())
            .doOnSubscribe {
                appWidgetManager.partiallyUpdateAppWidget(
                    widgetId,
                    PhotoFrameWidgetRemoteViews(applicationContext) {
                        setLoadingVisible(true)
                    }
                )
            }
            .doOnTerminate {
                appWidgetManager.partiallyUpdateAppWidget(
                    widgetId,
                    PhotoFrameWidgetRemoteViews(applicationContext) {
                        setLoadingVisible(false)
                    }
                )
            }
            .doOnSuccess {
                log.debug {
                    "createWork(): completed"
                }
            }
            .onErrorReturn { error ->
                log.error(error) {
                    "error_occurred:" +
                            "\nwidgetId=$widgetId"
                }

                Result.failure()
            }
    }

    companion object {
        const val TAG = "UpdatePhotoFrameWidget"
        private const val WIDGET_ID_KEY = "widget_id"

        fun getInputData(
            widgetId: Int,
        ) = Data.Builder()
            .putInt(WIDGET_ID_KEY, widgetId)
            .build()
    }
}
