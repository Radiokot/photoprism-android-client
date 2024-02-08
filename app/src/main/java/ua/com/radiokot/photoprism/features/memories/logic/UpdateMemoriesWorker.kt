package ua.com.radiokot.photoprism.features.memories.logic

import android.content.Context
import androidx.work.Data
import androidx.work.WorkerParameters
import androidx.work.rxjava3.RxWorker
import io.reactivex.rxjava3.core.Single
import org.koin.core.component.KoinComponent
import ua.com.radiokot.photoprism.di.DI_SCOPE_SESSION
import ua.com.radiokot.photoprism.extension.kLogger
import ua.com.radiokot.photoprism.util.LocalDate
import java.util.Calendar

/**
 * A worker meant to update the memories daily from the desired hour.
 * Must be scheduled hourly.
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

    override fun createWork(): Single<Result> {
        val currentHour = LocalDate().getCalendar()[Calendar.HOUR_OF_DAY]
        if (currentHour != startingFromHour) {
            log.debug {
                "createWork(): skip_due_to_hour_mismatch:" +
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
                "createWork(): skipp_due_to_missing_use_case"
            }

            return Single.just(Result.failure())
        }

        return useCase
            .invoke()
            .toSingleDefault(Result.success())
            .onErrorReturnItem(Result.retry())
    }

    companion object {
        const val TAG = "UpdateMemories"
        private const val STARTING_FROM_HOUR_KEY = "starting_from_hour"

        fun getInputData(startingFromHour: Int) = Data.Builder()
            .putInt(STARTING_FROM_HOUR_KEY, startingFromHour)
            .build()
    }
}
