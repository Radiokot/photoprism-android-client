package ua.com.radiokot.photoprism.features.importt

import org.koin.android.ext.koin.androidApplication
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.androidx.viewmodel.dsl.viewModelOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module
import ua.com.radiokot.photoprism.env.data.model.EnvSession
import ua.com.radiokot.photoprism.features.gallery.search.logic.SearchPredicates
import ua.com.radiokot.photoprism.features.importt.albums.data.model.ImportAlbum
import ua.com.radiokot.photoprism.features.importt.albums.view.model.ImportAlbumSelectionViewModel
import ua.com.radiokot.photoprism.features.importt.logic.ImportFilesUseCase
import ua.com.radiokot.photoprism.features.importt.logic.ParseImportIntentUseCase
import ua.com.radiokot.photoprism.features.importt.view.ImportNotificationsManager
import ua.com.radiokot.photoprism.features.importt.view.model.ImportViewModel
import ua.com.radiokot.photoprism.features.shared.albums.sharedAlbumsModule

val importFeatureModule = module {
    includes(sharedAlbumsModule)

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

        viewModel {
            ImportAlbumSelectionViewModel(
                albumsRepository = get(),
                searchPredicate = { album: ImportAlbum, query: String ->
                    SearchPredicates.generalCondition(query, album.title)
                },
                exactMatchPredicate = { album: ImportAlbum, query: String ->
                    album.title.equals(query, ignoreCase = true)
                },
            )
        }
    }

    single {
        ParseImportIntentUseCase.Factory(
            contentResolver = androidApplication().contentResolver,
        )
    } bind ParseImportIntentUseCase.Factory::class

    singleOf(::ImportNotificationsManager)
}
