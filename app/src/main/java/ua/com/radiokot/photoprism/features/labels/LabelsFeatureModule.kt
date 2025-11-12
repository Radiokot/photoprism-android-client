package ua.com.radiokot.photoprism.features.labels

import org.koin.dsl.bind
import org.koin.dsl.module
import ua.com.radiokot.photoprism.di.retrofitApiModule
import ua.com.radiokot.photoprism.env.data.model.EnvSession
import ua.com.radiokot.photoprism.features.labels.data.storage.LabelsRepository

val labelsFeatureModule = module {
    includes(
        retrofitApiModule,
    )

    scope<EnvSession> {
        scoped {
            LabelsRepository.Factory(
                photoPrismLabelsService = get(),
            )
        } bind LabelsRepository.Factory::class
    }
}
