package ua.com.radiokot.photoprism.features.importt.view.model

import android.Manifest
import android.app.Application
import android.content.Intent
import android.os.Build
import androidx.core.net.toUri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import io.reactivex.rxjava3.kotlin.toCompletable
import io.reactivex.rxjava3.schedulers.Schedulers
import io.reactivex.rxjava3.subjects.PublishSubject
import ua.com.radiokot.photoprism.di.JsonObjectMapper
import ua.com.radiokot.photoprism.env.data.model.EnvSession
import ua.com.radiokot.photoprism.extension.autoDispose
import ua.com.radiokot.photoprism.extension.isSelfPermissionGranted
import ua.com.radiokot.photoprism.extension.kLogger
import ua.com.radiokot.photoprism.extension.toMainThreadObservable
import ua.com.radiokot.photoprism.features.importt.albums.data.model.ImportAlbum
import ua.com.radiokot.photoprism.features.importt.logic.ImportFilesWorker
import ua.com.radiokot.photoprism.features.importt.logic.ParseImportIntentUseCase
import ua.com.radiokot.photoprism.features.importt.model.ImportableFile
import ua.com.radiokot.photoprism.features.shared.albums.data.storage.AlbumsRepository
import java.io.File

class ImportViewModel(
    private val parseImportIntentUseCaseFactory: ParseImportIntentUseCase.Factory,
    private val albumsRepository: AlbumsRepository,
    private val session: EnvSession,
    private val jsonObjectMapper: JsonObjectMapper,
    application: Application,
) : AndroidViewModel(application) {
    private val log = kLogger("ImportVM")
    private val context = getApplication<Application>()
    private var isInitialized = false
    private lateinit var files: List<ImportableFile>
    private val permissionsToCheckBeforeStart = mutableListOf<String>()
    private var albums: Set<ImportAlbum> = emptySet()
    private val uploadUniqueName = "${ImportFilesWorker.TAG}:${System.currentTimeMillis()}"

    val summary: MutableLiveData<Summary> = MutableLiveData()
    val isNotificationPermissionRationaleVisible: MutableLiveData<Boolean> = MutableLiveData(false)
    val isMediaPermissionRationaleVisible: MutableLiveData<Boolean> = MutableLiveData(false)
    private val eventsSubject = PublishSubject.create<Event>()
    val events = eventsSubject.toMainThreadObservable()
    val isStartButtonEnabled: MutableLiveData<Boolean> = MutableLiveData(true)

    fun initOnce(importIntent: Intent) {
        if (isInitialized) {
            return
        }

        files = parseImportIntentUseCaseFactory.get(importIntent).invoke()

        summary.value = Summary(
            libraryRootUrl = session.envConnectionParams.rootUrl.toString(),
            fileCount = files.size,
            sizeMb = files.sumOf(ImportableFile::size).toDouble() / (1024 * 1024),
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
            && !context.isSelfPermissionGranted(Manifest.permission.ACCESS_MEDIA_LOCATION)
        ) {
            isMediaPermissionRationaleVisible.value = true
            permissionsToCheckBeforeStart += Manifest.permission.ACCESS_MEDIA_LOCATION
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
            && !context.isSelfPermissionGranted(Manifest.permission.POST_NOTIFICATIONS)
        ) {
            isNotificationPermissionRationaleVisible.value = true
            permissionsToCheckBeforeStart += Manifest.permission.POST_NOTIFICATIONS
        }

        // Update albums now for faster selection.
        albumsRepository.updateIfNotFresh()

        isInitialized = true

        log.debug {
            "initOnce(): initialized:" +
                    "\nsummary=${summary.value}," +
                    "\npermissionsToCheckBeforeStart=$permissionsToCheckBeforeStart"
        }
    }

    fun onStartClicked() {
        if (permissionsToCheckBeforeStart.isNotEmpty()) {
            log.debug {
                "onStartClicked(): requesting_permissions_first"
            }

            eventsSubject.onNext(Event.RequestPermissions(permissionsToCheckBeforeStart.toTypedArray()))
        } else {
            log.debug {
                "onStartClicked(): starting_import_in_background"
            }

            startImportInBackgroundAndFinishAsync()
                .subscribe()
                .autoDispose(this)
        }
    }

    fun onCancelClicked() {
        log.debug {
            "onCancelClicked(): finishing"
        }

        eventsSubject.onNext(Event.Finish)
    }

    fun onAlbumsClicked() {
        log.debug {
            "onAlbumsClicked(): opening_selection"
        }

        eventsSubject.onNext(
            Event.OpenAlbumSelectionForResult(
                currentlySelectedAlbums = albums,
            )
        )
    }

    fun onAlbumSelectionResult(selectedAlbums: Set<ImportAlbum>) {
        log.debug {
            "onAlbumsSelected(): updating_selection:" +
                    "\nselectedAlbums=${selectedAlbums.size}"
        }

        this.albums = selectedAlbums

        summary.value = summary.value!!.copy(
            albums = selectedAlbums.map(ImportAlbum::title),
        )
    }

    fun onPermissionsResult(results: Map<String, Boolean>) {
        log.debug {
            "onPermissionsResult(): result_received:" +
                    "\nresults:${results.entries}"
        }

        log.debug {
            "onPermissionsResult(): starting_import_in_background"
        }

        startImportInBackgroundAndFinishAsync()
            .subscribe()
            .autoDispose(this)
    }

    private fun startImportInBackgroundAndFinishAsync() = {
        eventsSubject.onNext(Event.ShowStartedInBackgroundMessage)
        isStartButtonEnabled.postValue(false)

        // Allow reading the URIs after the activity is finished.
        files.forEach { file ->
            context.grantUriPermission(
                context.packageName,
                file.contentUri.toUri(),
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
        }

        // Write list of files to a file to overcome
        // the WorkManager data size limit.
        val fileListJsonFile = File(
            getApplication<Application>().noBackupFilesDir,
            "${uploadUniqueName}.json"
        )
        jsonObjectMapper.writeValue(fileListJsonFile, files)

        WorkManager.getInstance(context)
            .enqueueUniqueWork(
                uploadUniqueName,
                ExistingWorkPolicy.REPLACE,
                OneTimeWorkRequestBuilder<ImportFilesWorker>()
                    .setInputData(
                        ImportFilesWorker.getInputData(
                            fileListJsonFile = fileListJsonFile,
                            albums = albums,
                            jsonObjectMapper = jsonObjectMapper,
                        )
                    )
                    .addTag(ImportFilesWorker.TAG)
                    .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                    .build()
            )

        log.debug {
            "startImportInBackgroundAndFinish(): finishing_after_start"
        }

        eventsSubject.onNext(Event.Finish)
    }.toCompletable().subscribeOn(Schedulers.io())

    data class Summary(
        val libraryRootUrl: String,
        val fileCount: Int,
        val sizeMb: Double,
        val albums: Collection<String> = emptySet(),
    )

    sealed interface Event {
        object Finish : Event
        object ShowStartedInBackgroundMessage : Event

        /**
         * Request given [permissions] reporting the result
         * to the [onPermissionsResult] method.
         */
        class RequestPermissions(
            val permissions: Array<String>,
        ) : Event

        /**
         * Open import albums selection to get the result.
         *
         * [onAlbumSelectionResult] must be called when the result is obtained.
         */
        class OpenAlbumSelectionForResult(
            val currentlySelectedAlbums: Set<ImportAlbum>,
        ) : Event
    }
}
