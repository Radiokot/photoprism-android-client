package ua.com.radiokot.photoprism.features.importt.view.model

import android.app.Application
import android.content.Intent
import androidx.core.net.toUri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import io.reactivex.rxjava3.subjects.PublishSubject
import ua.com.radiokot.photoprism.di.JsonObjectMapper
import ua.com.radiokot.photoprism.env.data.model.EnvSession
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

    val summary: MutableLiveData<Summary> = MutableLiveData()
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

        isInitialized = true

        log.debug {
            "initOnce(): initialized:" +
                    "\nsummary=$summary"
        }
    }

    fun onStartClicked() {
        log.debug {
            "onStartClicked(): starting_worker"
        }

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
            "onStartClicked(): finishing"
        }

        eventsSubject.onNext(Event.Finish)
    }

    fun onCancelClicked() {
        log.debug {
            "onCancelClicked(): finishing"
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
    }
}
