package ua.com.radiokot.photoprism.features.gallery.di

import android.net.Uri
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.androidx.viewmodel.dsl.viewModelOf
import org.koin.core.module.Module
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.scopedOf
import org.koin.core.module.dsl.singleOf
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module
import ua.com.radiokot.photoprism.BuildConfig
import ua.com.radiokot.photoprism.di.EXTERNAL_DOWNLOADS_DIRECTORY
import ua.com.radiokot.photoprism.di.INTERNAL_DOWNLOADS_DIRECTORY
import ua.com.radiokot.photoprism.di.SelfParameterHolder
import ua.com.radiokot.photoprism.di.UTC_DAY_DATE_FORMAT
import ua.com.radiokot.photoprism.di.UTC_DAY_YEAR_DATE_FORMAT
import ua.com.radiokot.photoprism.di.UTC_MONTH_DATE_FORMAT
import ua.com.radiokot.photoprism.di.UTC_MONTH_YEAR_DATE_FORMAT
import ua.com.radiokot.photoprism.di.dateFormatModules
import ua.com.radiokot.photoprism.env.data.model.EnvSession
import ua.com.radiokot.photoprism.features.envconnection.di.envConnectionFeatureModules
import ua.com.radiokot.photoprism.features.gallery.data.storage.SimpleGalleryMediaRepository
import ua.com.radiokot.photoprism.features.gallery.logic.DownloadFileUseCase
import ua.com.radiokot.photoprism.features.gallery.logic.FileReturnIntentCreator
import ua.com.radiokot.photoprism.features.gallery.logic.MediaCodecVideoFormatSupport
import ua.com.radiokot.photoprism.features.gallery.logic.MediaFileDownloadUrlFactory
import ua.com.radiokot.photoprism.features.gallery.logic.MediaPreviewUrlFactory
import ua.com.radiokot.photoprism.features.gallery.logic.MediaWebUrlFactory
import ua.com.radiokot.photoprism.features.gallery.logic.PhotoPrismMediaDownloadUrlFactory
import ua.com.radiokot.photoprism.features.gallery.logic.PhotoPrismMediaWebUrlFactory
import ua.com.radiokot.photoprism.features.gallery.logic.PhotoPrismPreviewUrlFactory
import ua.com.radiokot.photoprism.features.gallery.search.di.gallerySearchFeatureModules
import ua.com.radiokot.photoprism.features.gallery.search.logic.TvDetector
import ua.com.radiokot.photoprism.features.gallery.search.logic.TvDetectorImpl
import ua.com.radiokot.photoprism.features.gallery.search.view.model.GallerySearchViewModel
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
        includes(gallerySearchFeatureModules)

        single {
            FileReturnIntentCreator(
                fileProviderAuthority = BuildConfig.FILE_PROVIDER_AUTHORITY,
                context = get(),
            )
        } bind FileReturnIntentCreator::class

        singleOf(::TvDetectorImpl) {
            bind<TvDetector>()
        }

        scope<EnvSession> {
            scoped {
                val session = get<EnvSession>()

                PhotoPrismPreviewUrlFactory(
                    apiUrl = session.envConnectionParams.apiUrl.toString(),
                    previewToken = session.previewToken,
                    videoFormatSupport = MediaCodecVideoFormatSupport()
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

            scopedOf(DownloadFileUseCase::Factory)

            // Downloader must be session-scoped to have the correct
            // HTTP client (e.g. for mTLS)
            scopedOf(::OkHttpObservableDownloader) {
                bind<ObservableDownloader>()
            }

            viewModelOf(::DownloadMediaFileViewModel)

            scoped {
                SimpleGalleryMediaRepository.Factory(
                    photoPrismPhotosService = get(),
                    thumbnailUrlFactory = get(),
                    downloadUrlFactory = get(),
                    webUrlFactory = get(),
                    pageLimit = 40,
                )
            } bind SimpleGalleryMediaRepository.Factory::class

            viewModelOf(::GallerySearchViewModel)

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
                    externalDownloadsDir = get(named(EXTERNAL_DOWNLOADS_DIRECTORY)),
                    downloadMediaFileViewModel = get(),
                    searchViewModel = get(),
                    fastScrollViewModel = get(),
                    disconnectFromEnvUseCase = get(),
                )
            }
        }
    },
)
