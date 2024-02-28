package ua.com.radiokot.photoprism.features.memories.di

import androidx.work.WorkManager
import org.koin.android.ext.koin.androidApplication
import org.koin.core.module.dsl.scopedOf
import org.koin.core.module.dsl.singleOf
import org.koin.core.qualifier._q
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module
import ua.com.radiokot.photoprism.api.config.service.PhotoPrismClientConfigService
import ua.com.radiokot.photoprism.base.data.storage.ObjectPersistence
import ua.com.radiokot.photoprism.db.DevAppDatabase
import ua.com.radiokot.photoprism.di.APP_NO_BACKUP_PREFERENCES
import ua.com.radiokot.photoprism.di.EnvPhotoPrismClientConfigServiceParams
import ua.com.radiokot.photoprism.di.ioModules
import ua.com.radiokot.photoprism.env.data.model.EnvSession
import ua.com.radiokot.photoprism.features.memories.data.storage.MemoriesDbDao
import ua.com.radiokot.photoprism.features.memories.data.storage.MemoriesRepository
import ua.com.radiokot.photoprism.features.memories.data.storage.UpdateMemoriesWorkerStatusPersistenceOnPrefs
import ua.com.radiokot.photoprism.features.memories.logic.CancelDailyMemoriesUpdatesUseCase
import ua.com.radiokot.photoprism.features.memories.logic.GetMemoriesUseCase
import ua.com.radiokot.photoprism.features.memories.logic.ScheduleDailyMemoriesUpdatesUseCase
import ua.com.radiokot.photoprism.features.memories.logic.UpdateMemoriesUseCase
import ua.com.radiokot.photoprism.features.memories.logic.UpdateMemoriesWorker
import ua.com.radiokot.photoprism.features.memories.view.MemoriesNotificationsManager
import ua.com.radiokot.photoprism.features.memories.view.model.GalleryMemoriesListViewModel

val memoriesFeatureModule = module {
    includes(ioModules)

    single {
        get<DevAppDatabase>().memories()
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

    single<ObjectPersistence<UpdateMemoriesWorker.Status>> {
        UpdateMemoriesWorkerStatusPersistenceOnPrefs(
            key = "update_memories_worker_status",
            preferences = get(named(APP_NO_BACKUP_PREFERENCES)),
            jsonObjectMapper = get(),
        )
    }

    singleOf(::MemoriesNotificationsManager)

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

        scopedOf(::GalleryMemoriesListViewModel)
    }
}
