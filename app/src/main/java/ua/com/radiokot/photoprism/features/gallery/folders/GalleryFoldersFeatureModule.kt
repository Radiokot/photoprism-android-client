package ua.com.radiokot.photoprism.features.gallery.folders

import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import ua.com.radiokot.photoprism.env.data.model.EnvSession
import ua.com.radiokot.photoprism.features.albums.albumsModule
import ua.com.radiokot.photoprism.features.gallery.folders.view.model.GalleryFolderViewModel

val galleryFoldersFeatureModule = module {
    includes(albumsModule)

    scope<EnvSession> {
        viewModel {
            GalleryFolderViewModel(
                galleryMediaRepositoryFactory = get(),
                archiveGalleryMediaUseCase = get(),
                deleteGalleryMediaUseCase = get(),
                listViewModel = get(),
                mediaFilesActionsViewModel = get(),
            )
        }
    }
}
