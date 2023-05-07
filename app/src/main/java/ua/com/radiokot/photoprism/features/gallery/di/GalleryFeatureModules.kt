package ua.com.radiokot.photoprism.features.gallery.di

import android.net.Uri
import android.text.format.DateFormat
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.module.Module
import org.koin.core.qualifier._q
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module
import ua.com.radiokot.photoprism.BuildConfig
import ua.com.radiokot.photoprism.db.AppDatabase
import ua.com.radiokot.photoprism.di.*
import ua.com.radiokot.photoprism.env.data.model.EnvSession
import ua.com.radiokot.photoprism.extension.useMonthsFromResources
import ua.com.radiokot.photoprism.features.gallery.data.storage.AlbumsRepository
import ua.com.radiokot.photoprism.features.gallery.data.storage.SearchBookmarksRepository
import ua.com.radiokot.photoprism.features.gallery.data.storage.SimpleGalleryMediaRepository
import ua.com.radiokot.photoprism.features.gallery.logic.*
import ua.com.radiokot.photoprism.features.gallery.view.model.*
import ua.com.radiokot.photoprism.util.downloader.ObservableDownloader
import ua.com.radiokot.photoprism.util.downloader.OkHttpObservableDownloader
import java.text.SimpleDateFormat
import java.util.*

private const val MONTH_DATE_FORMAT = "month"
private const val MONTH_YEAR_DATE_FORMAT = "month-year"
private const val DAY_DATE_FORMAT = "day"
private const val DAY_YEAR_DATE_FORMAT = "day-year"

class ImportSearchBookmarksUseCaseParams(
    val fileUri: Uri,
) : SelfParameterHolder()

val galleryFeatureModules: List<Module> = listOf(
    module {
        factory { Locale.getDefault() }

        factory(named(MONTH_DATE_FORMAT)) {
            SimpleDateFormat("MMMM", get<Locale>())
                .useMonthsFromResources(get())
        } bind java.text.DateFormat::class

        factory(named(MONTH_YEAR_DATE_FORMAT)) {
            SimpleDateFormat(
                DateFormat.getBestDateTimePattern(get(), "MMMMyyyy"),
                get<Locale>()
            ).useMonthsFromResources(get())
        } bind java.text.DateFormat::class

        factory(named(DAY_DATE_FORMAT)) {
            SimpleDateFormat(
                DateFormat.getBestDateTimePattern(get(), "EEMMMMd"),
                get<Locale>()
            )
        } bind java.text.DateFormat::class

        factory(named(DAY_YEAR_DATE_FORMAT)) {
            SimpleDateFormat(
                DateFormat.getBestDateTimePattern(get(), "EEMMMMdyyyy"),
                get<Locale>()
            )
        } bind java.text.DateFormat::class
    },

    module {
        includes(envModules)

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
                SimpleGalleryMediaRepository.Factory(
                    photoPrismPhotosService = get(),
                    thumbnailUrlFactory = get(),
                    downloadUrlFactory = get(),
                    webUrlFactory = get(),
                    pageLimit = 50,
                )
            } bind SimpleGalleryMediaRepository.Factory::class

            scoped {
                AlbumsRepository(
                    photoPrismAlbumsService = get(),
                    previewUrlFactory = get(),
                    types = listOf("album", "folder")
                )
            } bind AlbumsRepository::class

            viewModel {
                DownloadMediaFileViewModel(
                    downloadFileUseCaseFactory = DownloadFileUseCase.Factory(
                        observableDownloader = get(),
                    )
                )
            }

            viewModel {
                GallerySearchAlbumsViewModel(
                    albumsRepository = get(),
                )
            }

            viewModel {
                GallerySearchViewModel(
                    bookmarksRepository = get(),
                    albumsViewModel = get(),
                    searchFiltersGuideUrl = getProperty("searchFiltersGuideUrl")
                )
            }

            viewModel {
                GalleryFastScrollViewModel(
                    bubbleMonthYearDateFormat = get(named(MONTH_YEAR_DATE_FORMAT)),
                    bubbleMonthDateFormat = get(named(MONTH_DATE_FORMAT)),
                )
            }

            viewModel {
                GalleryViewModel(
                    galleryMediaRepositoryFactory = get(),
                    dateHeaderDayYearDateFormat = get(named(DAY_YEAR_DATE_FORMAT)),
                    dateHeaderDayDateFormat = get(named(DAY_DATE_FORMAT)),
                    dateHeaderMonthYearDateFormat = get(named(MONTH_YEAR_DATE_FORMAT)),
                    dateHeaderMonthDateFormat = get(named(MONTH_DATE_FORMAT)),
                    internalDownloadsDir = get(named(INTERNAL_DOWNLOADS_DIRECTORY)),
                    downloadMediaFileViewModel = get(),
                    searchViewModel = get(),
                    fastScrollViewModel = get(),
                )
            }
        }
    },

    module {
        single {
            FileReturnIntentCreator(
                fileProviderAuthority = BuildConfig.FILE_PROVIDER_AUTHORITY,
                context = get(),
            )
        } bind FileReturnIntentCreator::class

        scope<EnvSession> {
            // Downloader must be session-scoped to have the correct
            // HTTP client (e.g. for mTLS)
            scoped {
                OkHttpObservableDownloader(
                    httpClient = get()
                )
            } bind ObservableDownloader::class
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
)