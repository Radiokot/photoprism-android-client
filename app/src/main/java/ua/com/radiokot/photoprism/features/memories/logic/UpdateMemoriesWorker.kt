package ua.com.radiokot.photoprism.features.memories.logic

import android.content.Context
import androidx.work.WorkerParameters
import androidx.work.rxjava3.RxWorker
import io.reactivex.rxjava3.core.Single
import org.koin.core.component.KoinScopeComponent
import org.koin.core.component.get
import org.koin.core.scope.Scope
import ua.com.radiokot.photoprism.di.DI_SCOPE_SESSION
import ua.com.radiokot.photoprism.extension.tryOrNull

class UpdateMemoriesWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : RxWorker(appContext, workerParams), KoinScopeComponent {
    override val scope: Scope
        get() = getKoin().getScope(DI_SCOPE_SESSION)

    override fun createWork(): Single<Result> {
        // The use case may not be obtainable if not connected to an env.
        val useCase = tryOrNull { get<UpdateMemoriesUseCase>() }
            ?: return Single.just(Result.failure())

        return useCase
            .invoke()
            .toSingleDefault(Result.success())
            .onErrorReturnItem(Result.retry())
    }

    companion object {
        const val TAG = "UpdateMemories"
    }
}
