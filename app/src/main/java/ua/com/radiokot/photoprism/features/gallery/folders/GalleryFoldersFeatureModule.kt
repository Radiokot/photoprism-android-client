package ua.com.radiokot.photoprism.features.gallery.folders

import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import ua.com.radiokot.photoprism.env.data.model.EnvSession
import ua.com.radiokot.photoprism.features.gallery.folders.view.model.GalleryFolderViewModel
import ua.com.radiokot.photoprism.features.gallery.folders.view.model.GalleryFoldersViewModel
import ua.com.radiokot.photoprism.features.gallery.search.logic.SearchPredicates
import ua.com.radiokot.photoprism.features.shared.albums.data.model.Album

val galleryFoldersFeatureModule = module {
    scope<EnvSession> {
        viewModel {
            GalleryFoldersViewModel(
                albumsRepository = get(),
                searchPredicate = { album: Album, query: String ->
                    SearchPredicates.generalCondition(query, album.title)
                },
            )
        }

        viewModel {
            GalleryFolderViewModel(
                galleryMediaRepositoryFactory = get(),
                galleryPreferences = get(),
            )
        }
    }
}
