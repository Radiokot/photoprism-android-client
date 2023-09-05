package ua.com.radiokot.photoprism.features.gallery.di

import android.net.Uri
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.module.Module
import org.koin.core.qualifier._q
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module
import ua.com.radiokot.photoprism.BuildConfig
import ua.com.radiokot.photoprism.db.AppDatabase
import ua.com.radiokot.photoprism.di.APP_NO_BACKUP_PREFERENCES
import ua.com.radiokot.photoprism.di.INTERNAL_DOWNLOADS_DIRECTORY
import ua.com.radiokot.photoprism.di.INTERNAL_EXPORT_DIRECTORY
import ua.com.radiokot.photoprism.di.SelfParameterHolder
import ua.com.radiokot.photoprism.di.UTC_DAY_DATE_FORMAT
import ua.com.radiokot.photoprism.di.UTC_DAY_YEAR_DATE_FORMAT
import ua.com.radiokot.photoprism.di.UTC_MONTH_DATE_FORMAT
import ua.com.radiokot.photoprism.di.UTC_MONTH_YEAR_DATE_FORMAT
import ua.com.radiokot.photoprism.di.dateFormatModules
import ua.com.radiokot.photoprism.di.dbModules
import ua.com.radiokot.photoprism.di.ioModules
import ua.com.radiokot.photoprism.env.data.model.EnvSession
import ua.com.radiokot.photoprism.features.envconnection.di.envConnectionFeatureModules
import ua.com.radiokot.photoprism.features.gallery.data.storage.SearchBookmarksRepository
import ua.com.radiokot.photoprism.features.gallery.data.storage.SearchPreferences
import ua.com.radiokot.photoprism.features.gallery.data.storage.SearchPreferencesOnPrefs
import ua.com.radiokot.photoprism.features.gallery.data.storage.SimpleGalleryMediaRepository
import ua.com.radiokot.photoprism.features.gallery.logic.DownloadFileUseCase
import ua.com.radiokot.photoprism.features.gallery.logic.FileReturnIntentCreator
import ua.com.radiokot.photoprism.features.gallery.logic.MediaFileDownloadUrlFactory
import ua.com.radiokot.photoprism.features.gallery.logic.MediaPreviewUrlFactory
import ua.com.radiokot.photoprism.features.gallery.logic.MediaWebUrlFactory
import ua.com.radiokot.photoprism.features.gallery.logic.PhotoPrismMediaDownloadUrlFactory
import ua.com.radiokot.photoprism.features.gallery.logic.PhotoPrismMediaWebUrlFactory
import ua.com.radiokot.photoprism.features.gallery.logic.PhotoPrismPreviewUrlFactory
import ua.com.radiokot.photoprism.features.gallery.search.albums.data.model.Album
import ua.com.radiokot.photoprism.features.gallery.search.albums.data.storage.AlbumsRepository
import ua.com.radiokot.photoprism.features.gallery.search.albums.view.model.AlbumsOverviewViewModel
import ua.com.radiokot.photoprism.features.gallery.search.albums.view.model.GallerySearchAlbumsViewModel
import ua.com.radiokot.photoprism.features.gallery.search.logic.ExportSearchBookmarksUseCase
import ua.com.radiokot.photoprism.features.gallery.search.logic.ImportSearchBookmarksUseCase
import ua.com.radiokot.photoprism.features.gallery.search.logic.JsonSearchBookmarksBackup
import ua.com.radiokot.photoprism.features.gallery.search.logic.SearchBookmarksBackup
import ua.com.radiokot.photoprism.features.gallery.search.logic.SearchPredicates
import ua.com.radiokot.photoprism.features.gallery.search.logic.TvDetector
import ua.com.radiokot.photoprism.features.gallery.search.logic.TvDetectorImpl
import ua.com.radiokot.photoprism.features.gallery.search.people.data.model.Person
import ua.com.radiokot.photoprism.features.gallery.search.people.data.storage.PeopleRepository
import ua.com.radiokot.photoprism.features.gallery.search.people.view.model.GallerySearchPeopleViewModel
import ua.com.radiokot.photoprism.features.gallery.search.people.view.model.PeopleOverviewViewModel
import ua.com.radiokot.photoprism.features.gallery.search.view.model.GallerySearchViewModel
import ua.com.radiokot.photoprism.features.gallery.search.view.model.SearchBookmarkDialogViewModel
import ua.com.radiokot.photoprism.features.gallery.view.model.DownloadMediaFileViewModel
import ua.com.radiokot.photoprism.features.gallery.view.model.GalleryFastScrollViewModel
import ua.com.radiokot.photoprism.features.gallery.view.model.GalleryViewModel
import ua.com.radiokot.photoprism.util.downloader.ObservableDownloader
import ua.com.radiokot.photoprism.util.downloader.OkHttpObservableDownloader

class ImportSearchBookmarksUseCaseParams(
    val fileUri: Uri,
) : SelfParameterHolder()

val galleryFeatureModules: List<Module> = listOf(
    module {
        includes(envConnectionFeatureModules)
        includes(dateFormatModules)

        single {
            FileReturnIntentCreator(
                fileProviderAuthority = BuildConfig.FILE_PROVIDER_AUTHORITY,
                context = get(),
            )
        } bind FileReturnIntentCreator::class

        single {
            TvDetectorImpl(
                context = androidContext(),
            )
        } bind TvDetector::class

        scope<EnvSession> {
            scoped {
                val session = get<EnvSession>()

                PhotoPrismPreviewUrlFactory(
                    apiUrl = session.envConnectionParams.apiUrl.toString(),
                    previewToken = session.previewToken,
                )
            } bind MediaPreviewUrlFactory::class

            scoped {
                val session = get<EnvSession>()

                PhotoPrismMediaDownloadUrlFactory(
                    apiUrl = session.envConnectionParams.apiUrl.toString(),
                    downloadToken = session.downloadToken,
                )
            } bind MediaFileDownloadUrlFactory::class

            scoped {
                val session = get<EnvSession>()

                PhotoPrismMediaWebUrlFactory(
                    webLibraryUrl = session.envConnectionParams.webLibraryUrl,
                )
            } bind MediaWebUrlFactory::class

            scoped {
                DownloadFileUseCase.Factory(
                    observableDownloader = get(),
                    context = get(),
                )
            } bind DownloadFileUseCase.Factory::class

            scoped {
                SimpleGalleryMediaRepository.Factory(
                    photoPrismPhotosService = get(),
                    thumbnailUrlFactory = get(),
                    downloadUrlFactory = get(),
                    webUrlFactory = get(),
                    pageLimit = 40,
                )
            } bind SimpleGalleryMediaRepository.Factory::class

            scoped {
                AlbumsRepository(
                    photoPrismAlbumsService = get(),
                    previewUrlFactory = get(),
                    types = listOf("album", "folder"),
                    comparator = compareByDescending(Album::isFavorite)
                        .thenBy(Album::title)
                )
            } bind AlbumsRepository::class

            scoped {
                PeopleRepository(
                    photoPrismFacesService = get(),
                    photoPrismSubjectsService = get(),
                    previewUrlFactory = get(),
                )
            } bind PeopleRepository::class

            // Downloader must be session-scoped to have the correct
            // HTTP client (e.g. for mTLS)
            scoped {
                OkHttpObservableDownloader(
                    httpClient = get()
                )
            } bind ObservableDownloader::class

            viewModel {
                DownloadMediaFileViewModel(
                    downloadFileUseCaseFactory = get(),
                )
            }

            viewModel {
                GallerySearchAlbumsViewModel(
                    albumsRepository = get(),
                    searchPreferences = get(),
                )
            }

            viewModel {
                GallerySearchPeopleViewModel(
                    peopleRepository = get(),
                    searchPreferences = get(),
                )
            }

            viewModel {
                GallerySearchViewModel(
                    bookmarksRepository = get(),
                    albumsViewModel = get(),
                    peopleViewModel = get(),
                    searchPreferences = get(),
                )
            }

            viewModel {
                AlbumsOverviewViewModel(
                    albumsRepository = get(),
                    searchPredicate = { album: Album, query: String ->
                        SearchPredicates.generalCondition(query, album.title)
                    }
                )
            }

            viewModel {
                PeopleOverviewViewModel(
                    peopleRepository = get(),
                    searchPredicate = { person: Person, query: String ->
                        SearchPredicates.generalCondition(query, person.name)
                    }
                )
            }

            viewModel {
                GalleryFastScrollViewModel(
                    bubbleUtcMonthYearDateFormat = get(named(UTC_MONTH_YEAR_DATE_FORMAT)),
                    bubbleUtcMonthDateFormat = get(named(UTC_MONTH_DATE_FORMAT)),
                )
            }

            viewModel {
                GalleryViewModel(
                    galleryMediaRepositoryFactory = get(),
                    dateHeaderUtcDayYearDateFormat = get(named(UTC_DAY_YEAR_DATE_FORMAT)),
                    dateHeaderUtcDayDateFormat = get(named(UTC_DAY_DATE_FORMAT)),
                    dateHeaderUtcMonthYearDateFormat = get(named(UTC_MONTH_YEAR_DATE_FORMAT)),
                    dateHeaderUtcMonthDateFormat = get(named(UTC_MONTH_DATE_FORMAT)),
                    internalDownloadsDir = get(named(INTERNAL_DOWNLOADS_DIRECTORY)),
                    downloadMediaFileViewModel = get(),
                    searchViewModel = get(),
                    fastScrollViewModel = get(),
                    disconnectFromEnvUseCase = get(),
                )
            }
        }
    },

    module {
        includes(dbModules)

        single {
            SearchBookmarksRepository(
                bookmarksDbDao = get<AppDatabase>().bookmarks(),
            )
        } bind SearchBookmarksRepository::class

        viewModel {
            SearchBookmarkDialogViewModel(
                bookmarksRepository = get(),
            )
        }
    },

    module {
        includes(ioModules)

        single {
            JsonSearchBookmarksBackup(jsonObjectMapper = get())
        } bind SearchBookmarksBackup::class

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
