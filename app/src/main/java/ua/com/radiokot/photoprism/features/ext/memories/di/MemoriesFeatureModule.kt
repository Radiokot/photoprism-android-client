package ua.com.radiokot.photoprism.features.ext.memories.di

import org.koin.core.module.dsl.scopedOf
import org.koin.core.module.dsl.singleOf
import org.koin.core.qualifier._q
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module
import ua.com.radiokot.photoprism.api.config.service.PhotoPrismClientConfigService
import ua.com.radiokot.photoprism.base.data.storage.ObjectPersistence
import ua.com.radiokot.photoprism.db.AppDatabase
import ua.com.radiokot.photoprism.di.APP_NO_BACKUP_PREFERENCES
import ua.com.radiokot.photoprism.di.EnvPhotoPrismClientConfigServiceParams
import ua.com.radiokot.photoprism.di.ioModules
import ua.com.radiokot.photoprism.env.data.model.EnvSession
import ua.com.radiokot.photoprism.features.ext.memories.data.storage.MemoriesDbDao
import ua.com.radiokot.photoprism.features.ext.memories.data.storage.MemoriesPreferences
import ua.com.radiokot.photoprism.features.ext.memories.data.storage.MemoriesPreferencesOnPrefs
import ua.com.radiokot.photoprism.features.ext.memories.data.storage.MemoriesRepository
import ua.com.radiokot.photoprism.features.ext.memories.data.storage.UpdateMemoriesWorkerStatusPersistenceOnPrefs
import ua.com.radiokot.photoprism.features.ext.memories.logic.CancelDailyMemoriesUpdatesUseCase
import ua.com.radiokot.photoprism.features.ext.memories.logic.GetMemoriesUseCase
import ua.com.radiokot.photoprism.features.ext.memories.logic.ScheduleDailyMemoriesUpdatesUseCase
import ua.com.radiokot.photoprism.features.ext.memories.logic.UpdateMemoriesUseCase
import ua.com.radiokot.photoprism.features.ext.memories.logic.UpdateMemoriesWorker
import ua.com.radiokot.photoprism.features.ext.memories.view.MemoriesNotificationsManager
import ua.com.radiokot.photoprism.features.ext.memories.view.model.GalleryMemoriesListViewModel

val memoriesFeatureModule = module {
    includes(ioModules)

    single {
        get<AppDatabase>().memories()
    } bind MemoriesDbDao::class

    single {
        ScheduleDailyMemoriesUpdatesUseCase(
            workManager = get(),
            startingFromHour = 8, // Start from 8:00
        )
    } bind ScheduleDailyMemoriesUpdatesUseCase::class

    singleOf(::CancelDailyMemoriesUpdatesUseCase)

    single<ObjectPersistence<UpdateMemoriesWorker.Status>>(_q<UpdateMemoriesWorker.Status>()) {
        UpdateMemoriesWorkerStatusPersistenceOnPrefs(
            key = "update_memories_worker_status",
            preferences = get(named(APP_NO_BACKUP_PREFERENCES)),
            jsonObjectMapper = get(),
        )
    }

    single {
        // Instance to be used out of the session scope.
        MemoriesNotificationsManager(
            context = get(),
            picasso = null,
            memoriesPreferences = get(),
        )
    } bind MemoriesNotificationsManager::class

    single {
        MemoriesPreferencesOnPrefs(
            preferences = get(named(APP_NO_BACKUP_PREFERENCES)),
            keyPrefix = "ext_memories",
        )
    } bind MemoriesPreferences::class

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
                galleryMediaRepositoryFactory = get(),
                previewUrlFactory = get(),
            )
        } bind GetMemoriesUseCase::class

        scopedOf(::MemoriesRepository)

        scopedOf(::UpdateMemoriesUseCase)

        scopedOf(::GalleryMemoriesListViewModel)

        scopedOf(::MemoriesNotificationsManager)
    }
}
