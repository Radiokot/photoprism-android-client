package ua.com.radiokot.photoprism.features.memories.di

import androidx.work.WorkManager
import org.koin.android.ext.koin.androidApplication
import org.koin.core.module.dsl.scopedOf
import org.koin.core.module.dsl.singleOf
import org.koin.core.qualifier._q
import org.koin.dsl.bind
import org.koin.dsl.module
import ua.com.radiokot.photoprism.api.config.service.PhotoPrismClientConfigService
import ua.com.radiokot.photoprism.db.AppDatabase
import ua.com.radiokot.photoprism.di.EnvPhotoPrismClientConfigServiceParams
import ua.com.radiokot.photoprism.env.data.model.EnvSession
import ua.com.radiokot.photoprism.features.memories.data.storage.MemoriesDbDao
import ua.com.radiokot.photoprism.features.memories.data.storage.MemoriesRepository
import ua.com.radiokot.photoprism.features.memories.logic.CancelDailyMemoriesUpdatesUseCase
import ua.com.radiokot.photoprism.features.memories.logic.GetMemoriesUseCase
import ua.com.radiokot.photoprism.features.memories.logic.ScheduleDailyMemoriesUpdatesUseCase
import ua.com.radiokot.photoprism.features.memories.logic.UpdateMemoriesUseCase

val memoriesFeatureModule = module {
    single {
        get<AppDatabase>().memories()
    } bind MemoriesDbDao::class

    single {
        WorkManager.getInstance(androidApplication())
    } bind WorkManager::class

    single {
        ScheduleDailyMemoriesUpdatesUseCase(
            workManager = get(),
            startingFromHour = 8, // Start from 8:00
        )
    } bind ScheduleDailyMemoriesUpdatesUseCase::class

    singleOf(::CancelDailyMemoriesUpdatesUseCase)

    scope<EnvSession> {
        scoped {
            val session = get<EnvSession>()
            val photoPrismClientConfigService: PhotoPrismClientConfigService =
                get(_q<EnvPhotoPrismClientConfigServiceParams>()) {
                    EnvPhotoPrismClientConfigServiceParams(
                        envConnectionParams = session.envConnectionParams,
                        sessionId = session.id,
                    )
                }
            GetMemoriesUseCase(
                photoPrismClientConfigService = photoPrismClientConfigService,
                photoPrismPhotosService = get(),
                previewUrlFactory = get(),
            )
        } bind GetMemoriesUseCase::class

        scopedOf(::MemoriesRepository)

        scopedOf(::UpdateMemoriesUseCase)
    }
}
