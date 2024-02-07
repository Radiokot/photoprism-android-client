package ua.com.radiokot.photoprism.features.memories.logic

import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import ua.com.radiokot.photoprism.extension.kLogger
import java.util.concurrent.TimeUnit

/**
 * @param startingFromHour – from which hour of a day to start the update, 0-23.
 * The exact update launch time is chosen by Android till the end of the day.
 */
class ScheduleDailyMemoriesUpdatesUseCase(
    private val workManager: WorkManager,
    private val startingFromHour: Int,
) {
    private val log = kLogger("ScheduleDailyMemoryUpdatesUC")

    init {
        require(startingFromHour in (0..23)) {
            "The starting hour is out of the 23-hour range"
        }
    }

    private val constraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    private val workRequest = PeriodicWorkRequestBuilder<UpdateMemoriesWorker>(
        // Run once a day.
        1, TimeUnit.DAYS,
        // During the left allowed hours.
        (24 - startingFromHour.toLong()), TimeUnit.HOURS,
    )
        .setConstraints(constraints)
        .addTag(UpdateMemoriesWorker.TAG)
        .build()

    operator fun invoke() = with(workManager) {
        enqueueUniquePeriodicWork(
            UpdateMemoriesWorker.TAG,
            ExistingPeriodicWorkPolicy.UPDATE,
            workRequest,
        )

        log.debug {
            "invoke(): scheduled:" +
                    "\nstartingFrom=$startingFromHour:00"
        }
    }
}
