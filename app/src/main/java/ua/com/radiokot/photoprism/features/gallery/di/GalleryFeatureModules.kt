package ua.com.radiokot.photoprism.features.gallery.di

import android.content.Context
import android.text.format.DateFormat
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module
import ua.com.radiokot.photoprism.BuildConfig
import ua.com.radiokot.photoprism.db.AppDatabase
import ua.com.radiokot.photoprism.di.dbModules
import ua.com.radiokot.photoprism.di.envModules
import ua.com.radiokot.photoprism.env.data.model.EnvSession
import ua.com.radiokot.photoprism.features.gallery.data.storage.SearchBookmarksRepository
import ua.com.radiokot.photoprism.features.gallery.data.storage.SimpleGalleryMediaRepository
import ua.com.radiokot.photoprism.features.gallery.logic.*
import ua.com.radiokot.photoprism.features.gallery.view.model.DownloadMediaFileViewModel
import ua.com.radiokot.photoprism.features.gallery.view.model.GallerySearchViewModel
import ua.com.radiokot.photoprism.features.gallery.view.model.GalleryViewModel
import ua.com.radiokot.photoprism.features.gallery.view.model.SearchBookmarkDialogViewModel
import ua.com.radiokot.photoprism.util.downloader.ObservableDownloader
import ua.com.radiokot.photoprism.util.downloader.OkHttpObservableDownloader
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

const val INTERNAL_DOWNLOADS_DIRECTORY = "internal-downloads"

val galleryFeatureModules: List<Module> = listOf(
    module {
        includes(envModules)

        scope<EnvSession> {
            scoped {
                val session = get<EnvSession>()

                PhotoPrismPreviewUrlFactory(
                    apiUrl = session.apiUrl,
                    previewToken = session.previewToken,
                )
            }.bind(MediaPreviewUrlFactory::class)

            scoped {
                val session = get<EnvSession>()

                PhotoPrismMediaDownloadUrlFactory(
                    apiUrl = session.apiUrl,
                    downloadToken = session.downloadToken,
                )
            }.bind(MediaFileDownloadUrlFactory::class)

            scoped {
                SimpleGalleryMediaRepository.Factory(
                    photoPrismPhotosService = get(),
                    thumbnailUrlFactory = get(),
                    downloadUrlFactory = get(),
                    pageLimit = 50,
                )
            }.bind(SimpleGalleryMediaRepository.Factory::class)

            viewModel {
                DownloadMediaFileViewModel(
                    downloadFileUseCaseFactory = DownloadFileUseCase.Factory(
                        observableDownloader = get()
                    )
                )
            }

            viewModel {
                GallerySearchViewModel(
                    bookmarksRepository = get(),
                    searchFiltersGuideUrl = getProperty("searchFiltersGuideUrl")
                )
            }

            viewModel {
                val locale = Locale.getDefault()

                GalleryViewModel(
                    galleryMediaRepositoryFactory = get(),
                    dateHeaderDayYearDateFormat = SimpleDateFormat(
                        DateFormat.getBestDateTimePattern(locale, "EEMMMMdyyyy"),
                        locale
                    ),
                    dateHeaderDayDateFormat = SimpleDateFormat(
                        DateFormat.getBestDateTimePattern(locale, "EEMMMMd"),
                        locale
                    ),
                    internalDownloadsDir = get(named(INTERNAL_DOWNLOADS_DIRECTORY)),
                )
            }
        }
    },

    module {
        single(named(INTERNAL_DOWNLOADS_DIRECTORY)) {
            // See file_provider_paths.
            File(get<Context>().filesDir.absolutePath + "/downloads")
                .apply { mkdirs() }
        } bind File::class

        single {
            OkHttpObservableDownloader(
                httpClient = get()
            )
        }.bind(ObservableDownloader::class)

        single {
            FileReturnIntentCreator(
                fileProviderAuthority = BuildConfig.FILE_PROVIDER_AUTHORITY,
                context = get(),
            )
        }.bind(FileReturnIntentCreator::class)
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
    }
)