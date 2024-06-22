package ua.com.radiokot.photoprism.features.importt.view.model

import android.Manifest
import android.app.Application
import android.content.Intent
import android.os.Build
import androidx.core.net.toUri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import io.reactivex.rxjava3.subjects.PublishSubject
import ua.com.radiokot.photoprism.di.JsonObjectMapper
import ua.com.radiokot.photoprism.env.data.model.EnvSession
import ua.com.radiokot.photoprism.extension.isSelfPermissionGranted
import ua.com.radiokot.photoprism.extension.kLogger
import ua.com.radiokot.photoprism.extension.toMainThreadObservable
import ua.com.radiokot.photoprism.features.importt.logic.ImportFilesWorker
import ua.com.radiokot.photoprism.features.importt.logic.ParseImportIntentUseCase
import ua.com.radiokot.photoprism.features.importt.model.ImportableFile

class ImportViewModel(
    private val parseImportIntentUseCaseFactory: ParseImportIntentUseCase.Factory,
    private val session: EnvSession,
    private val jsonObjectMapper: JsonObjectMapper,
    application: Application,
) : AndroidViewModel(application) {
    private val log = kLogger("ImportVM")
    private val context = getApplication<Application>()
    private var isInitialized = false
    private lateinit var files: List<ImportableFile>
    private val permissionsToCheckBeforeStart = mutableListOf<String>()

    val summary: MutableLiveData<Summary> = MutableLiveData()
    val isNotificationPermissionRationaleVisible: MutableLiveData<Boolean> = MutableLiveData(false)
    val isMediaPermissionRationaleVisible: MutableLiveData<Boolean> = MutableLiveData(false)
    private val eventsSubject = PublishSubject.create<Event>()
    val events = eventsSubject.toMainThreadObservable()

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

        isInitialized = true

        log.debug {
            "initOnce(): initialized:" +
                    "\nsummary=$summary," +
                    "\npermissionsToCheckBeforeStart=$permissionsToCheckBeforeStart"
        }
    }

    fun onStartClicked() {
        if (permissionsToCheckBeforeStart.isNotEmpty()) {
            log.debug {
                "onStartClicked(): checking_permissions_first"
            }

            eventsSubject.onNext(Event.CheckPermissions(permissionsToCheckBeforeStart.toTypedArray()))
        } else {
            log.debug {
                "onStartClicked(): starting_import_in_background"
            }

            startImportInBackgroundAndFinish()
        }
    }

    fun onCancelClicked() {
        log.debug {
            "onCancelClicked(): finishing"
        }

        eventsSubject.onNext(Event.Finish)
    }

    fun onPermissionsResult(results: Map<String, Boolean>) {
        log.debug {
            "onPermissionsResult(): result_received:" +
                    "\nresults:${results.entries}"
        }

        log.debug {
            "onPermissionsResult(): starting_import_in_background"
        }

        startImportInBackgroundAndFinish()
    }

    private fun startImportInBackgroundAndFinish() {
        // Allow reading the URIs after the activity is finished.
        files.forEach { file ->
            context.grantUriPermission(
                context.packageName,
                file.contentUri.toUri(),
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
        }

        WorkManager.getInstance(context)
            .enqueue(
                OneTimeWorkRequestBuilder<ImportFilesWorker>()
                    .setInputData(
                        ImportFilesWorker.getInputData(
                            files = files,
                            jsonObjectMapper = jsonObjectMapper,
                        )
                    )
                    .addTag(ImportFilesWorker.TAG)
                    .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                    .build()
            )

        eventsSubject.onNext(Event.ShowStartedInBackgroundMessage)

        log.debug {
            "startImportInBackgroundAndFinish(): finishing_after_start"
        }

        eventsSubject.onNext(Event.Finish)
    }

    data class Summary(
        val libraryRootUrl: String,
        val fileCount: Int,
        val sizeMb: Double,
    )

    sealed interface Event {
        object Finish : Event
        object ShowStartedInBackgroundMessage : Event

        /**
         * Request given [permissions] reporting the result
         * to the [onPermissionsResult] method.
         */
        class CheckPermissions(
            val permissions: Array<String>,
        ) : Event
    }
}
