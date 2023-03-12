package ua.com.radiokot.photoprism.features.gallery.di

import android.content.Context
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.module.Module
import org.koin.dsl.bind
import org.koin.dsl.module
import ua.com.radiokot.photoprism.BuildConfig
import ua.com.radiokot.photoprism.api.PhotoPrismSession
import ua.com.radiokot.photoprism.features.gallery.data.storage.SimpleGalleryMediaRepository
import ua.com.radiokot.photoprism.features.gallery.logic.DownloadFileUseCase
import ua.com.radiokot.photoprism.features.gallery.logic.FileReturnIntentCreator
import ua.com.radiokot.photoprism.features.gallery.logic.MediaThumbnailUrlFactory
import ua.com.radiokot.photoprism.features.gallery.logic.PhotoPrismThumbnailUrlFactory
import ua.com.radiokot.photoprism.features.gallery.view.GalleryViewModel
import ua.com.radiokot.photoprism.util.downloader.ObservableDownloader
import ua.com.radiokot.photoprism.util.downloader.OkHttpObservableDownloader
import java.io.File

val galleryFeatureModules: List<Module> = listOf(
    module {
        scope<PhotoPrismSession> {
            scoped {
                val session = get<PhotoPrismSession>()

                PhotoPrismThumbnailUrlFactory(
                    apiUrl = getProperty("apiUrl"),
                    previewToken = session.previewToken,
                )
            }.bind(MediaThumbnailUrlFactory::class)

            scoped {
                SimpleGalleryMediaRepository(
                    photoPrismPhotosService = get(),
                    thumbnailUrlFactory = get(),
                    pageLimit = 50,
                )
            }
        }
    },

    module {
        scope<PhotoPrismSession> {
            viewModel {
                GalleryViewModel(
                    galleryMediaRepository = get(),
                    // See file_provider_paths.
                    downloadsDir = File(get<Context>().filesDir.absolutePath + "/downloads")
                        .apply { mkdirs() },
                    downloadFileUseCaseFactory = DownloadFileUseCase.Factory(
                        observableDownloader = get()
                    )
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