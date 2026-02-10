package ua.com.radiokot.photoprism.features.gallery.search

import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.core.qualifier._q
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module
import ua.com.radiokot.photoprism.db.AppDatabase
import ua.com.radiokot.photoprism.di.APP_NO_BACKUP_PREFERENCES
import ua.com.radiokot.photoprism.di.INTERNAL_EXPORT_DIRECTORY
import ua.com.radiokot.photoprism.di.ioModules
import ua.com.radiokot.photoprism.env.data.model.EnvSession
import ua.com.radiokot.photoprism.features.albums.albumsFeatureModule
import ua.com.radiokot.photoprism.features.albums.data.model.Album
import ua.com.radiokot.photoprism.features.gallery.ImportSearchBookmarksUseCaseParams
import ua.com.radiokot.photoprism.features.gallery.data.storage.SearchBookmarksRepository
import ua.com.radiokot.photoprism.features.gallery.search.albums.view.model.GallerySearchAlbumSelectionViewModel
import ua.com.radiokot.photoprism.features.gallery.search.albums.view.model.GallerySearchAlbumsViewModel
import ua.com.radiokot.photoprism.features.gallery.search.data.storage.SearchPreferences
import ua.com.radiokot.photoprism.features.gallery.search.data.storage.SearchPreferencesOnPrefs
import ua.com.radiokot.photoprism.features.gallery.search.logic.ExportSearchBookmarksUseCase
import ua.com.radiokot.photoprism.features.gallery.search.logic.ImportSearchBookmarksUseCase
import ua.com.radiokot.photoprism.features.gallery.search.logic.JsonSearchBookmarksBackup
import ua.com.radiokot.photoprism.features.gallery.search.logic.SearchBookmarksBackup
import ua.com.radiokot.photoprism.features.gallery.search.logic.SearchPredicates
import ua.com.radiokot.photoprism.features.gallery.search.people.view.model.GallerySearchPeopleViewModel
import ua.com.radiokot.photoprism.features.gallery.search.view.model.SearchBookmarkDialogViewModel
import ua.com.radiokot.photoprism.features.people.peopleFeatureModule

val gallerySearchFeatureModules: List<Module> = listOf(
    // Bookmarks.
    module {
        single {
            SearchBookmarksRepository(
                bookmarksDbDao = get<AppDatabase>().bookmarks(),
            )
        } bind SearchBookmarksRepository::class

        viewModelOf(::SearchBookmarkDialogViewModel)

        // Import/export.
        singleOf(::JsonSearchBookmarksBackup) bind SearchBookmarksBackup::class

        factory {
            ExportSearchBookmarksUseCase(
                exportDir = get(named(INTERNAL_EXPORT_DIRECTORY)),
                backupStrategy = get(),
                fileReturnIntentCreator = get(),
                searchBookmarksRepository = get(),
            )
        } bind ExportSearchBookmarksUseCase::class

        factory(_q<ImportSearchBookmarksUseCaseParams>()) { params ->
            params as ImportSearchBookmarksUseCaseParams

            ImportSearchBookmarksUseCase(
                fileUri = params.fileUri,
                backupStrategy = get(),
                searchBookmarksRepository = get(),
                contentResolver = androidContext().contentResolver,
            )
        } bind ImportSearchBookmarksUseCase::class
    },

    // Albums.
    module {
        includes(albumsFeatureModule)

        scope<EnvSession> {
            viewModelOf(::GallerySearchAlbumsViewModel)

            viewModel {
                GallerySearchAlbumSelectionViewModel(
                    albumsRepositoryFactory = get(),
                    defaultSort = get(),
                    searchPredicate = { album: Album, query: String ->
                        SearchPredicates.generalCondition(query, album.title)
                    },
                    searchPreferences = get(),
                    previewUrlFactory = get(),
                )
            }
        }
    },

    // People.
    module {
        includes(peopleFeatureModule)

        scope<EnvSession> {
            viewModelOf(::GallerySearchPeopleViewModel)
        }
    },

    // Preferences.
    module {
        includes(ioModules)

        single {
            SearchPreferencesOnPrefs(
                preferences = get(named(APP_NO_BACKUP_PREFERENCES)),
                keyPrefix = "search",
            )
        } bind SearchPreferences::class
    },
)
