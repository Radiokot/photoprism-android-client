package ua.com.radiokot.photoprism.features.importt

import org.koin.android.ext.koin.androidApplication
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.bind
import org.koin.dsl.module
import ua.com.radiokot.photoprism.env.data.model.EnvSession
import ua.com.radiokot.photoprism.features.albums.albumsFeatureModule
import ua.com.radiokot.photoprism.features.importt.logic.ImportFilesUseCase
import ua.com.radiokot.photoprism.features.importt.logic.ParseImportIntentUseCase
import ua.com.radiokot.photoprism.features.importt.view.ImportNotificationsManager
import ua.com.radiokot.photoprism.features.importt.view.model.ImportViewModel

val importFeatureModule = module {
    includes(albumsFeatureModule)

    scope<EnvSession> {
        scoped {
            ImportFilesUseCase(
                contentResolver = androidApplication().contentResolver,
                photoPrismSessionService = get(),
                photoPrismUploadService = get(),
                albumsRepository = getOrNull(),
            )
        } bind ImportFilesUseCase::class

        viewModelOf(::ImportViewModel)
    }

    single {
        ParseImportIntentUseCase(
            contentResolver = androidApplication().contentResolver,
        )
    } bind ParseImportIntentUseCase::class

    singleOf(::ImportNotificationsManager)
}
