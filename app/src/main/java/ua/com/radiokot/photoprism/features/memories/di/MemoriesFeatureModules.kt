package ua.com.radiokot.photoprism.features.memories.di

import org.koin.core.module.Module
import org.koin.core.qualifier._q
import org.koin.dsl.bind
import org.koin.dsl.module
import ua.com.radiokot.photoprism.api.config.service.PhotoPrismClientConfigService
import ua.com.radiokot.photoprism.di.EnvPhotoPrismClientConfigServiceParams
import ua.com.radiokot.photoprism.env.data.model.EnvSession
import ua.com.radiokot.photoprism.features.memories.logic.GetMemoriesUseCase

val memoriesFeatureModules: List<Module> = listOf(
    module {
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
        }
    },
)
