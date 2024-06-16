package ua.com.radiokot.photoprism.features.importt.logic

import android.content.Context
import android.widget.Toast
import androidx.work.Data
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
    private val files: List<ImportableFile> by lazy {
        jsonObjectMapper
            .readerForListOf(ImportableFile::class.java)
            .readValue(workerParams.inputData.getString(FILES_JSON_KEY))
    }

    override fun createWork(): Single<Result> {
        if (scope.id != DI_SCOPE_SESSION) {
            log.debug {
                "createWork(): skip_as_missing_session_scope"
            }

            return Single.just(Result.success())
        }

        return importFilesUseCaseFactory.get(
            files = files,
            uploadToken = System.currentTimeMillis().toString(),
        )
            .invoke()
            .onErrorReturn { error ->
                log.error(error) {
                    "createWork(): error_occurred"
                }

                Result.failure()
            }
            .defaultIfEmpty(Result.success())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSuccess { result ->
                if (result is Result.Success) {
                    Toast.makeText(
                        applicationContext,
                        "Successfully uploaded ${files.size} files",
                        Toast.LENGTH_SHORT
                    ).show()
                } else if (result is Result.Failure) {
                    Toast.makeText(
                        applicationContext,
                        "Failed uploading ${files.size} files",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
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
