package ua.com.radiokot.photoprism.features.memories.logic

import android.content.Context
import androidx.work.Data
import androidx.work.WorkerParameters
import androidx.work.rxjava3.RxWorker
import io.reactivex.rxjava3.core.Single
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import ua.com.radiokot.photoprism.base.data.storage.ObjectPersistence
import ua.com.radiokot.photoprism.di.DI_SCOPE_SESSION
import ua.com.radiokot.photoprism.extension.kLogger
import ua.com.radiokot.photoprism.features.memories.view.MemoriesNotificationsManager
import ua.com.radiokot.photoprism.util.LocalDate
import java.util.Calendar

/**
 * A worker meant to update the memories daily from the desired hour.
 * To work reliably, must be scheduled to have multiple intervals per day.
 * The update is skipped if had a successful update this day or it is too early.
 *
 * @see getInputData
 */
class UpdateMemoriesWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : RxWorker(appContext, workerParams), KoinComponent {
    private val log = kLogger("UpdateMemoriesWorker")

    private val startingFromHour =
        workerParams.inputData.getInt(STARTING_FROM_HOUR_KEY, 0)
    private val statusPersistence: ObjectPersistence<Status> by inject()
    private val memoriesNotificationsManager: MemoriesNotificationsManager by inject()

    override fun createWork(): Single<Result> {
        val status = statusPersistence.loadItem() ?: Status()
        val (currentHour, currentDay) = LocalDate().getCalendar().let {
            it[Calendar.HOUR_OF_DAY] to it[Calendar.DAY_OF_YEAR]
        }

        if (currentDay == status.lastSuccessfulUpdateDay) {
            log.debug {
                "createWork(): skip_as_already_updated_today:" +
                        "\nlastSuccessfulUpdateDay=${status.lastSuccessfulUpdateDay}"
            }

            return Single.just(Result.failure())
        }

        if (currentHour < startingFromHour) {
            log.debug {
                "createWork(): skip_as_too_early:" +
                        "\ncurrentHour=$currentHour," +
                        "\nstartingFromHour=$startingFromHour"
            }

            return Single.just(Result.failure())
        }

        // The use case may not be obtainable if not connected to an env
        // hence there is no session scope.
        val useCase = getKoin().getScopeOrNull(DI_SCOPE_SESSION)
            ?.getOrNull<UpdateMemoriesUseCase>()
        if (useCase == null) {
            log.debug {
                "createWork(): skip_as_missing_use_case"
            }

            return Single.just(Result.failure())
        }

        return useCase
            .invoke()
            .doOnSuccess { foundMemories ->
                statusPersistence.saveItem(
                    status.copy(
                        lastSuccessfulUpdateDay = currentDay,
                    )
                )

                if (foundMemories.isNotEmpty()) {
                    memoriesNotificationsManager.notifyNewMemories(
                        bigPictureUrl = foundMemories.last().getThumbnailUrl(
                            viewSizePx = 500,
                        )
                    )
                }
            }
            .map { Result.success() }
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
