package ua.com.radiokot.photoprism.features.gallery.search.di

import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.androidx.viewmodel.dsl.viewModelOf
import org.koin.core.module.Module
import org.koin.core.module.dsl.scopedOf
import org.koin.core.module.dsl.singleOf
import org.koin.core.qualifier._q
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module
import ua.com.radiokot.photoprism.db.AppDatabase
import ua.com.radiokot.photoprism.di.APP_NO_BACKUP_PREFERENCES
import ua.com.radiokot.photoprism.di.INTERNAL_EXPORT_DIRECTORY
import ua.com.radiokot.photoprism.di.ioModules
import ua.com.radiokot.photoprism.env.data.model.EnvSession
import ua.com.radiokot.photoprism.extension.autoDispose
import ua.com.radiokot.photoprism.features.gallery.data.storage.SearchBookmarksRepository
import ua.com.radiokot.photoprism.features.gallery.data.storage.SearchPreferences
import ua.com.radiokot.photoprism.features.gallery.data.storage.SearchPreferencesOnPrefs
import ua.com.radiokot.photoprism.features.gallery.di.ImportSearchBookmarksUseCaseParams
import ua.com.radiokot.photoprism.features.gallery.search.albums.data.model.Album
import ua.com.radiokot.photoprism.features.gallery.search.albums.data.storage.AlbumsRepository
import ua.com.radiokot.photoprism.features.gallery.search.albums.view.model.GallerySearchAlbumSelectionViewModel
import ua.com.radiokot.photoprism.features.gallery.search.albums.view.model.GallerySearchAlbumsViewModel
import ua.com.radiokot.photoprism.features.gallery.search.logic.ExportSearchBookmarksUseCase
import ua.com.radiokot.photoprism.features.gallery.search.logic.ImportSearchBookmarksUseCase
import ua.com.radiokot.photoprism.features.gallery.search.logic.JsonSearchBookmarksBackup
import ua.com.radiokot.photoprism.features.gallery.search.logic.SearchBookmarksBackup
import ua.com.radiokot.photoprism.features.gallery.search.logic.SearchPredicates
import ua.com.radiokot.photoprism.features.gallery.search.people.data.model.Person
import ua.com.radiokot.photoprism.features.gallery.search.people.data.storage.PeopleRepository
import ua.com.radiokot.photoprism.features.gallery.search.people.view.model.GallerySearchPeopleViewModel
import ua.com.radiokot.photoprism.features.gallery.search.people.view.model.GallerySearchPeopleSelectionViewModel
import ua.com.radiokot.photoprism.features.gallery.search.view.model.SearchBookmarkDialogViewModel

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
        scope<EnvSession> {
            scoped {
                AlbumsRepository(
                    photoPrismAlbumsService = get(),
                    previewUrlFactory = get(),
                ).also { repository ->
                    val searchPreferences = get<SearchPreferences>()

                    // Subscribe the repo to the folders preference.
                    searchPreferences
                        .showAlbumFolders
                        .subscribe(repository::includeFolders::set)
                        .autoDispose(this@scoped)
                }
            } bind AlbumsRepository::class

            viewModelOf(::GallerySearchAlbumsViewModel)

            viewModel {
                GallerySearchAlbumSelectionViewModel(
                    albumsRepository = get(),
                    searchPredicate = { album: Album, query: String ->
                        SearchPredicates.generalCondition(query, album.title)
                    }
                )
            }
        }
    },

    // People.
    module {
        scope<EnvSession> {
            scopedOf(::PeopleRepository)

            viewModelOf(::GallerySearchPeopleViewModel)

            viewModel {
                GallerySearchPeopleSelectionViewModel(
                    peopleRepository = get(),
                    searchPredicate = { person: Person, query: String ->
                        SearchPredicates.generalCondition(query, person.name)
                    }
                )
            }
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
