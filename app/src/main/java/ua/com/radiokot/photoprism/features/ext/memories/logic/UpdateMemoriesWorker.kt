package ua.com.radiokot.photoprism.features.ext.memories.logic

import android.content.Context
import androidx.work.Data
import androidx.work.WorkerParameters
import androidx.work.rxjava3.RxWorker
import io.reactivex.rxjava3.core.Single
import org.koin.core.component.KoinScopeComponent
import org.koin.core.component.createScope
import org.koin.core.component.inject
import org.koin.core.scope.Scope
import ua.com.radiokot.photoprism.base.data.storage.ObjectPersistence
import ua.com.radiokot.photoprism.di.DI_SCOPE_SESSION
import ua.com.radiokot.photoprism.extension.kLogger
import ua.com.radiokot.photoprism.features.ext.memories.view.MemoriesNotificationsManager
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
) : RxWorker(appContext, workerParams), KoinScopeComponent {
    override val scope: Scope by lazy {
        // Prefer the session scope, but allow running without it.
        getKoin().getScopeOrNull(DI_SCOPE_SESSION) ?: createScope()
    }

    private val log = kLogger("UpdateMemoriesWorker")

    private val startingFromHour =
        workerParams.inputData.getInt(STARTING_FROM_HOUR_KEY, 0)
    private val statusPersistence: ObjectPersistence<Status> by inject()
    private val updateMemoriesUseCase: UpdateMemoriesUseCase by inject()
    private val memoriesNotificationsManager: MemoriesNotificationsManager by inject()

    override fun createWork(): Single<Result> {
        if (scope.id != DI_SCOPE_SESSION) {
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
                ) {
                    log.debug {
                        "createWork(): notify_new_memories"
                    }

                    memoriesNotificationsManager.notifyNewMemories(
                        bigPictureUrl = foundMemories.last().getThumbnailUrl(
                            viewSizePx = 500,
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
