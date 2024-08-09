package ua.com.radiokot.photoprism.features.importt.logic

import android.app.PendingIntent
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.work.Data
import androidx.work.ForegroundInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.rxjava3.RxWorker
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.scope.Scope
import ua.com.radiokot.photoprism.di.DI_SCOPE_SESSION
import ua.com.radiokot.photoprism.di.JsonObjectMapper
import ua.com.radiokot.photoprism.extension.kLogger
import ua.com.radiokot.photoprism.extension.toSingle
import ua.com.radiokot.photoprism.features.importt.albums.data.model.ImportAlbum
import ua.com.radiokot.photoprism.features.importt.model.ImportableFile
import ua.com.radiokot.photoprism.features.importt.model.sizeMb
import ua.com.radiokot.photoprism.features.importt.view.ImportNotificationsManager
import java.io.File
import java.util.concurrent.TimeUnit

class ImportFilesWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : RxWorker(appContext, workerParams),
    KoinComponent {

    private val sessionScope: Scope?
        get() = getKoin().getScopeOrNull(DI_SCOPE_SESSION)
    private val log = kLogger("ImportFilesWorker")
    private val cancelIntent: PendingIntent by lazy {
        WorkManager.getInstance(appContext).createCancelPendingIntent(id)
    }
    private val jsonObjectMapper: JsonObjectMapper by inject()
    private val importNotificationsManager: ImportNotificationsManager by inject()
    private val fileListJsonFile: File by lazy {
        workerParams.inputData.getString(FILE_LIST_JSON_PATH)
            ?.let(::File)
            ?: error("Missing $FILE_LIST_JSON_PATH")
    }
    private val albums: Set<ImportAlbum> by lazy {
        jsonObjectMapper
            .readerForListOf(ImportAlbum::class.java)
            .readValue<Collection<ImportAlbum>>(workerParams.inputData.getString(ALBUMS_JSON_KEY))
            .toSet()
    }
    private val uploadToken = System.currentTimeMillis().toString()
    private var importStatus: ImportFilesUseCase.Status =
        ImportFilesUseCase.Status.Uploading.INDETERMINATE

    override fun createWork(): Single<Result> {
        val importFilesUseCase = sessionScope?.get<ImportFilesUseCase>()

        if (importFilesUseCase == null) {
            log.debug {
                "createWork(): skip_as_missing_session_scope"
            }

            return Single.just(Result.success())
        }

        lateinit var files: Collection<ImportableFile>

        return readFilesFromFile()
            .flatMapObservable { readFiles ->
                files = readFiles

                importFilesUseCase(
                    files = readFiles,
                    albums = albums,
                    uploadToken = uploadToken,
                )
            }
            .throttleLast(500, TimeUnit.MILLISECONDS)
            .switchMapCompletable { status ->
                importStatus = status
                foregroundInfo.flatMapCompletable(::setForeground)
            }
            .toSingleDefault(Result.success())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSubscribe {
                log.debug {
                    "createWork(): starting:" +
                            "\nfileListJson=$fileListJsonFile," +
                            "\nalbums=$albums," +
                            "\nuploadToken=$uploadToken"
                }
            }
            .doOnTerminate {
                try {
                    fileListJsonFile.delete()

                    log.debug {
                        "createWork(): file_list_json_deleted:" +
                                "\nfileListJson=$fileListJsonFile"
                    }
                } catch (e: Exception) {
                    log.warn(e) {
                        "createWork(): failed_deleting_file_list_file"
                    }
                }
            }
            .doOnError { error ->
                log.error(error) {
                    "createWork(): error_occurred"
                }

                if (!isStopped) {
                    importNotificationsManager.notifyFailedImport(
                        uploadToken = uploadToken,
                    )
                }
            }
            .doOnSuccess {
                log.debug {
                    "createWork(): completed"
                }

                if (!isStopped) {
                    importNotificationsManager.notifySuccessfulImport(
                        uploadToken = uploadToken,
                        fileCount = files.size,
                        sizeMb = files.sizeMb,
                    )
                }
            }
            .onErrorReturnItem(Result.failure())
    }

    private fun readFilesFromFile(): Single<List<ImportableFile>> = {
        check(fileListJsonFile.canRead()) {
            "File list JSON file is not readable: $fileListJsonFile"
        }

        jsonObjectMapper
            .readerForListOf(ImportableFile::class.java)
            .readValue<List<ImportableFile>>(fileListJsonFile)
    }.toSingle().subscribeOn(Schedulers.io())

    override fun getForegroundInfo(): Single<ForegroundInfo> {
        val notification = importNotificationsManager.getImportProgressNotification(
            progressPercent = when (val status = importStatus) {
                is ImportFilesUseCase.Status.Uploading ->
                    status.percent

                ImportFilesUseCase.Status.ProcessingUpload ->
                    -1.0
            },
            cancelIntent = cancelIntent,
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
        private const val ALBUMS_JSON_KEY = "albums"
        private const val FILE_LIST_JSON_PATH = "files_file"

        /**
         * @param fileListJsonFile a JSON file containing a list of [ImportableFile],
         * which will be deleted on task end.
         */
        fun getInputData(
            fileListJsonFile: File,
            albums: Set<ImportAlbum>,
            jsonObjectMapper: JsonObjectMapper,
        ) = Data.Builder()
            .putString(FILE_LIST_JSON_PATH, fileListJsonFile.path)
            // Albums must be converted to a typed array to overcome Java type erasure,
            // which makes @JsonTypeInfo useless when serializing a collection.
            .putString(ALBUMS_JSON_KEY, jsonObjectMapper.writeValueAsString(albums.toTypedArray()))
            .build()
    }
}
