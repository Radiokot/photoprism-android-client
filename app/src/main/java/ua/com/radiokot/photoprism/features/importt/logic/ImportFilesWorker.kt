package ua.com.radiokot.photoprism.features.importt.logic

import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.work.Data
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.rxjava3.RxWorker
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Single
import org.koin.core.component.KoinScopeComponent
import org.koin.core.component.createScope
import org.koin.core.component.inject
import org.koin.core.scope.Scope
import ua.com.radiokot.photoprism.di.DI_SCOPE_SESSION
import ua.com.radiokot.photoprism.di.JsonObjectMapper
import ua.com.radiokot.photoprism.extension.kLogger
import ua.com.radiokot.photoprism.features.importt.model.ImportableFile
import ua.com.radiokot.photoprism.features.importt.view.ImportNotificationsManager

class ImportFilesWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : RxWorker(appContext, workerParams),
    KoinScopeComponent {
    override val scope: Scope by lazy {
        // Prefer the session scope, but allow running without it.
        getKoin().getScopeOrNull(DI_SCOPE_SESSION) ?: createScope()
    }

    private val log = kLogger("ImportFilesWorker")

    private val jsonObjectMapper: JsonObjectMapper by inject()
    private val importFilesUseCaseFactory: ImportFilesUseCase.Factory by inject()
    private val importNotificationsManager: ImportNotificationsManager by inject()
    private val files: List<ImportableFile> by lazy {
        jsonObjectMapper
            .readerForListOf(ImportableFile::class.java)
            .readValue(workerParams.inputData.getString(FILES_JSON_KEY))
    }
    private val uploadToken = System.currentTimeMillis().toString()

    override fun createWork(): Single<Result> {
        if (scope.id != DI_SCOPE_SESSION) {
            log.debug {
                "createWork(): skip_as_missing_session_scope"
            }

            return Single.just(Result.success())
        }

        val useCase = importFilesUseCaseFactory.get(
            files = files,
            uploadToken = uploadToken,
        )

        return foregroundInfo
            .flatMapCompletable(::setForeground)
            .doOnComplete {
                log.debug {
                    "createWork(): foreground_set"
                }
            }
            .andThen(useCase.invoke())
            .onErrorReturn { error ->
                log.error(error) {
                    "createWork(): error_occurred"
                }

                Result.failure()
            }
            .defaultIfEmpty(Result.success())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSuccess { result ->
                log.debug {
                    "createWork(): complete_with_result:" +
                            "\nresult=$result"
                }

                if (result is Result.Success) {
                    importNotificationsManager.notifySuccessfulImport(
                        uploadToken = uploadToken,
                    )
                } else {
                    importNotificationsManager.notifyFailedImport(
                        uploadToken = uploadToken,
                    )
                }
            }
    }

    override fun getForegroundInfo(): Single<ForegroundInfo> {
        val notification = importNotificationsManager.getImportProgressNotification(
            progressPercent = -1.0,
        )
        val notificationId = ImportNotificationsManager.getImportProgressNotificationId(
            uploadToken = uploadToken,
        )

        return Single.just(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                ForegroundInfo(
                    notificationId,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                )
            else
                ForegroundInfo(
                    notificationId,
                    notification,
                )
        )
    }

    companion object {
        const val TAG = "ImportFiles"
        private const val FILES_JSON_KEY = "files"

        fun getInputData(
            files: List<ImportableFile>,
            jsonObjectMapper: JsonObjectMapper,
        ) = Data.Builder()
            .putString(FILES_JSON_KEY, jsonObjectMapper.writeValueAsString(files))
            .build()
    }
}
