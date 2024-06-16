package ua.com.radiokot.photoprism.features.importt

import org.koin.android.ext.koin.androidApplication
import org.koin.core.module.dsl.singleOf
import org.koin.core.qualifier._q
import org.koin.dsl.bind
import org.koin.dsl.module
import ua.com.radiokot.photoprism.api.session.service.PhotoPrismSessionService
import ua.com.radiokot.photoprism.di.EnvPhotoPrismSessionServiceParams
import ua.com.radiokot.photoprism.env.data.model.EnvSession
import ua.com.radiokot.photoprism.features.importt.logic.ImportFilesUseCase
import ua.com.radiokot.photoprism.features.importt.logic.ParseImportIntentUseCase
import ua.com.radiokot.photoprism.features.importt.view.ImportNotificationsManager

val importFeatureModule = module {
    scope<EnvSession> {
        scoped {
            val session = get<EnvSession>()
            val photoPrismSessionService =
                get<PhotoPrismSessionService>(_q<EnvPhotoPrismSessionServiceParams>()) {
                    EnvPhotoPrismSessionServiceParams(
                        envConnectionParams = session.envConnectionParams,
                    )
                }

            ImportFilesUseCase.Factory(
                contentResolver = androidApplication().contentResolver,
                photoPrismSessionService = photoPrismSessionService,
                photoPrismUploadService = get(),
            )
        } bind ImportFilesUseCase.Factory::class
    }

    single {
        ParseImportIntentUseCase.Factory(
            contentResolver = androidApplication().contentResolver,
        )
    } bind ParseImportIntentUseCase.Factory::class

    singleOf(::ImportNotificationsManager)
}
