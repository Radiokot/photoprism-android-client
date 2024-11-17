package ua.com.radiokot.photoprism.features.ext.memories.logic

import android.content.Context
import androidx.work.Data
import androidx.work.WorkerParameters
import androidx.work.rxjava3.RxWorker
import io.reactivex.rxjava3.core.Single
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.qualifier._q
import org.koin.core.scope.Scope
import ua.com.radiokot.photoprism.base.data.storage.ObjectPersistence
import ua.com.radiokot.photoprism.di.DI_SCOPE_SESSION
import ua.com.radiokot.photoprism.extension.kLogger
import ua.com.radiokot.photoprism.features.ext.memories.data.storage.MemoriesPreferences
import ua.com.radiokot.photoprism.features.ext.memories.view.MemoriesNotificationsManager
import ua.com.radiokot.photoprism.features.gallery.logic.MediaPreviewUrlFactory
import ua.com.radiokot.photoprism.util.LocalDate
import java.util.Calendar

/**
 * A worker meant to update the memories daily from the desired hour.
 * To work reliably, must be scheduled to have multiple intervals per day.
 * The update is skipped if had a successful update this day or it is too early.
 * The worker runs even if the memories are disabled in preferences so the memories
 * appear instantly once re-enabled.
 *
 * @see getInputData
 */
class UpdateMemoriesWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : RxWorker(appContext, workerParams), KoinComponent {

    private val sessionScope: Scope?
        get() = getKoin().getScopeOrNull(DI_SCOPE_SESSION)
    private val log = kLogger("UpdateMemoriesWorker")
    private val startingFromHour =
        workerParams.inputData.getInt(STARTING_FROM_HOUR_KEY, 0)
    private val statusPersistence: ObjectPersistence<Status> by inject(_q<Status>())
    private val memoriesPreferences: MemoriesPreferences by inject()

    override fun createWork(): Single<Result> {
        val updateMemoriesUseCase = sessionScope?.get<UpdateMemoriesUseCase>()
        val memoriesNotificationsManager = sessionScope?.get<MemoriesNotificationsManager>()
        val previewUrlFactory = sessionScope?.get<MediaPreviewUrlFactory>()

        if (updateMemoriesUseCase == null
            || memoriesNotificationsManager == null
            || previewUrlFactory == null
        ) {
            log.debug {
                "createWork(): skip_as_missing_session_scope"
            }

            return Single.just(Result.success())
        }

        val status = statusPersistence.loadItem() ?: Status()
        val (currentHour, currentDay) = LocalDate().getCalendar().let {
            it[Calendar.HOUR_OF_DAY] to it[Calendar.DAY_OF_YEAR]
        }

        if (currentDay == status.lastSuccessfulUpdateDay) {
            log.debug {
                "createWork(): skip_as_already_updated_today:" +
                        "\nlastSuccessfulUpdateDay=${status.lastSuccessfulUpdateDay}"
            }

            return Single.just(Result.success())
        }

        if (currentHour < startingFromHour) {
            log.debug {
                "createWork(): skip_as_too_early:" +
                        "\ncurrentHour=$currentHour," +
                        "\nstartingFromHour=$startingFromHour"
            }

            return Single.just(Result.success())
        }

        return updateMemoriesUseCase
            .invoke()
            .doOnSuccess {
                statusPersistence.saveItem(
                    status.copy(
                        lastSuccessfulUpdateDay = currentDay,
                    )
                )
            }
            .flatMap { foundMemories ->
                if (foundMemories.isNotEmpty()
                    && memoriesNotificationsManager.areMemoriesNotificationsEnabled
                    && memoriesPreferences.isEnabled.value == true
                ) {
                    log.debug {
                        "createWork(): notify_new_memories"
                    }

                    memoriesNotificationsManager.notifyNewMemories(
                        bigPictureUrl = previewUrlFactory.getThumbnailUrl(
                            thumbnailHash = foundMemories.last().thumbnailHash,
                            sizePx = 500,
                        )
                    )
                } else {
                    log.debug {
                        "createWork(): skip_notify_as_disabled"
                    }

                    Single.just(Unit)
                }
            }
            .map { Result.success() }
            .doOnError {
                log.error(it) {
                    "createWork(): error_occurred"
                }
            }
            .onErrorReturnItem(Result.retry())
    }

    data class Status(
        /**
         * Day of the year when the last successful update has been performed.
         * 0 if there was no successful update.
         *
         * @see Calendar.DAY_OF_YEAR
         */
        val lastSuccessfulUpdateDay: Int = 0,
    )

    companion object {
        const val TAG = "UpdateMemories"
        private const val STARTING_FROM_HOUR_KEY = "starting_from_hour"

        fun getInputData(startingFromHour: Int) = Data.Builder()
            .putInt(STARTING_FROM_HOUR_KEY, startingFromHour)
            .build()
    }
}
