package ua.com.radiokot.photoprism.features.gallery.di

import android.content.Context
import android.text.format.DateFormat
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.module.Module
import org.koin.dsl.bind
import org.koin.dsl.module
import ua.com.radiokot.photoprism.BuildConfig
import ua.com.radiokot.photoprism.features.env.data.model.EnvSession
import ua.com.radiokot.photoprism.features.gallery.data.storage.SimpleGalleryMediaRepository
import ua.com.radiokot.photoprism.features.gallery.logic.*
import ua.com.radiokot.photoprism.features.gallery.view.model.DownloadMediaFileViewModel
import ua.com.radiokot.photoprism.features.gallery.view.model.GallerySearchViewModel
import ua.com.radiokot.photoprism.features.gallery.view.model.GalleryViewModel
import ua.com.radiokot.photoprism.util.downloader.ObservableDownloader
import ua.com.radiokot.photoprism.util.downloader.OkHttpObservableDownloader
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

val galleryFeatureModules: List<Module> = listOf(
    module {
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
                    apiUrl = session.previewToken,
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
            }
        }
    },

    module {
        scope<EnvSession> {
            viewModel {
                DownloadMediaFileViewModel(
                    // See file_provider_paths.
                    downloadsDir = File(get<Context>().filesDir.absolutePath + "/downloads")
                        .apply { mkdirs() },
                    downloadFileUseCaseFactory = DownloadFileUseCase.Factory(
                        observableDownloader = get()
                    )
                )
            }

            viewModel {
                GallerySearchViewModel()
            }

            viewModel {
                val locale = Locale.getDefault()

                GalleryViewModel(
                    galleryMediaRepositoryFactory = get(),
                    // TODO: Fix 'Y' for SDK 22, try 'yyyy'
                    dateHeaderDayYearDateFormat = SimpleDateFormat(
                        DateFormat.getBestDateTimePattern(locale, "EEMMMMdYYYY"),
                        locale
                    ),
                    dateHeaderDayDateFormat = SimpleDateFormat(
                        DateFormat.getBestDateTimePattern(locale, "EEMMMMd"),
                        locale
                    ),
                )
            }
        }
    },

    module {
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
        }
    }
)