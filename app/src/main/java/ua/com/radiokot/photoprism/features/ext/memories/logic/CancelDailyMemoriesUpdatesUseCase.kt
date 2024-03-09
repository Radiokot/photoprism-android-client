package ua.com.radiokot.photoprism.features.ext.memories.logic

import androidx.work.WorkManager

class CancelDailyMemoriesUpdatesUseCase(
    private val workManager: WorkManager,
) {
    operator fun invoke() =
        workManager.cancelAllWorkByTag(UpdateMemoriesWorker.TAG)
}
