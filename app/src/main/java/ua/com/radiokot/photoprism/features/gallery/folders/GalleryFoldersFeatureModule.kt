package ua.com.radiokot.photoprism.features.gallery.folders

import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module
import ua.com.radiokot.photoprism.di.APP_NO_BACKUP_PREFERENCES
import ua.com.radiokot.photoprism.env.data.model.EnvSession
import ua.com.radiokot.photoprism.features.albums.albumsModule
import ua.com.radiokot.photoprism.features.albums.data.model.Album
import ua.com.radiokot.photoprism.features.gallery.folders.data.storage.GalleryFoldersPreferences
import ua.com.radiokot.photoprism.features.gallery.folders.data.storage.GalleryFoldersPreferencesOnPrefs
import ua.com.radiokot.photoprism.features.gallery.folders.view.model.GalleryFolderViewModel
import ua.com.radiokot.photoprism.features.gallery.folders.view.model.GalleryFoldersViewModel
import ua.com.radiokot.photoprism.features.gallery.search.logic.SearchPredicates

val galleryFoldersFeatureModule = module {
    includes(albumsModule)

    single {
        GalleryFoldersPreferencesOnPrefs(
            defaultSort = get(),
            preferences = get(named(APP_NO_BACKUP_PREFERENCES)),
            jsonObjectMapper = get(),
        )
    } bind GalleryFoldersPreferences::class

    scope<EnvSession> {
        viewModel {
            GalleryFoldersViewModel(
                albumsRepository = get(),
                preferences = get(),
                searchPredicate = { album: Album, query: String ->
                    val fields = buildList {
                        add(album.title)
                        album.path
                            ?.split('/')
                            ?.filterNot(String::isEmpty)
                            ?.also(::addAll)
                    }

                    SearchPredicates.generalCondition(query, fields)
                },
            )
        }

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
