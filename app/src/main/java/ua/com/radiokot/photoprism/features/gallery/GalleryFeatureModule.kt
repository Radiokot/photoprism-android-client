package ua.com.radiokot.photoprism.features.gallery

import android.net.Uri
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.androidx.viewmodel.dsl.viewModelOf
import org.koin.core.module.dsl.scopedOf
import org.koin.core.module.dsl.singleOf
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module
import ua.com.radiokot.photoprism.BuildConfig
import ua.com.radiokot.photoprism.di.APP_NO_BACKUP_PREFERENCES
import ua.com.radiokot.photoprism.di.EXTERNAL_DOWNLOADS_DIRECTORY
import ua.com.radiokot.photoprism.di.INTERNAL_DOWNLOADS_DIRECTORY
import ua.com.radiokot.photoprism.di.SelfParameterHolder
import ua.com.radiokot.photoprism.di.dateFormatModule
import ua.com.radiokot.photoprism.env.data.model.EnvSession
import ua.com.radiokot.photoprism.features.envconnection.di.envConnectionFeatureModule
import ua.com.radiokot.photoprism.features.gallery.data.storage.GalleryPreferences
import ua.com.radiokot.photoprism.features.gallery.data.storage.GalleryPreferencesOnPrefs
import ua.com.radiokot.photoprism.features.gallery.data.storage.SimpleGalleryMediaRepository
import ua.com.radiokot.photoprism.features.gallery.logic.AddGalleryMediaToAlbumUseCase
import ua.com.radiokot.photoprism.features.gallery.logic.ArchiveGalleryMediaUseCase
import ua.com.radiokot.photoprism.features.gallery.logic.DeleteGalleryMediaUseCase
import ua.com.radiokot.photoprism.features.gallery.logic.DownloadFileUseCase
import ua.com.radiokot.photoprism.features.gallery.logic.FileReturnIntentCreator
import ua.com.radiokot.photoprism.features.gallery.logic.MediaCodecVideoFormatSupport
import ua.com.radiokot.photoprism.features.gallery.logic.MediaFileDownloadUrlFactory
import ua.com.radiokot.photoprism.features.gallery.logic.MediaPreviewUrlFactory
import ua.com.radiokot.photoprism.features.gallery.logic.MediaWebUrlFactory
import ua.com.radiokot.photoprism.features.gallery.logic.PhotoPrismMediaFileDownloadUrlFactory
import ua.com.radiokot.photoprism.features.gallery.logic.PhotoPrismMediaPreviewUrlFactory
import ua.com.radiokot.photoprism.features.gallery.logic.PhotoPrismMediaWebUrlFactory
import ua.com.radiokot.photoprism.features.gallery.logic.RemoveGalleryMediaFromAlbumUseCase
import ua.com.radiokot.photoprism.features.gallery.search.gallerySearchFeatureModules
import ua.com.radiokot.photoprism.features.gallery.search.logic.TvDetector
import ua.com.radiokot.photoprism.features.gallery.search.logic.TvDetectorImpl
import ua.com.radiokot.photoprism.features.gallery.search.view.model.GallerySearchViewModel
import ua.com.radiokot.photoprism.features.gallery.view.model.GalleryFastScrollViewModel
import ua.com.radiokot.photoprism.features.gallery.view.model.GalleryListViewModel
import ua.com.radiokot.photoprism.features.gallery.view.model.GalleryListViewModelImpl
import ua.com.radiokot.photoprism.features.gallery.view.model.GalleryMediaRemoteActionsViewModelDelegate
import ua.com.radiokot.photoprism.features.gallery.view.model.GalleryMediaRemoteActionsViewModelDelegateImpl
import ua.com.radiokot.photoprism.features.gallery.view.model.GallerySingleRepositoryViewModelGallery
import ua.com.radiokot.photoprism.features.gallery.view.model.GalleryViewModelGallery
import ua.com.radiokot.photoprism.features.gallery.view.model.GalleryMediaDownloadActionsViewModelDelegate
import ua.com.radiokot.photoprism.features.gallery.view.model.GalleryMediaDownloadActionsViewModelDelegateImpl
import ua.com.radiokot.photoprism.features.viewer.logic.BackgroundMediaFileDownloadManager
import ua.com.radiokot.photoprism.features.viewer.logic.ThreadPoolBackgroundMediaFileDownloadManager
import ua.com.radiokot.photoprism.util.downloader.ObservableDownloader
import ua.com.radiokot.photoprism.util.downloader.OkHttpObservableDownloader

class ImportSearchBookmarksUseCaseParams(
    val fileUri: Uri,
) : SelfParameterHolder()

val galleryFeatureModule = module {
    includes(envConnectionFeatureModule)
    includes(dateFormatModule)
    includes(gallerySearchFeatureModules)

    single {
        FileReturnIntentCreator(
            fileProviderAuthority = BuildConfig.FILE_PROVIDER_AUTHORITY,
            context = get(),
        )
    } bind FileReturnIntentCreator::class

    singleOf(::TvDetectorImpl) bind TvDetector::class

    single {
        GalleryPreferencesOnPrefs(
            preferences = get(named(APP_NO_BACKUP_PREFERENCES)),
            keyPrefix = "gallery"
        )
    } bind GalleryPreferences::class

    scope<EnvSession> {
        scoped {
            val session = get<EnvSession>()

            PhotoPrismMediaPreviewUrlFactory(
                apiUrl = session.envConnectionParams.apiUrl.toString(),
                previewToken = session.previewToken,
                videoFormatSupport = MediaCodecVideoFormatSupport()
            )
        } bind MediaPreviewUrlFactory::class

        scoped {
            val session = get<EnvSession>()

            PhotoPrismMediaFileDownloadUrlFactory(
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

        scopedOf(::DownloadFileUseCase)

        // Downloader must be session-scoped to have the correct
        // HTTP client (e.g. for mTLS)
        scopedOf(::OkHttpObservableDownloader) bind ObservableDownloader::class

        scoped {
            SimpleGalleryMediaRepository.Factory(
                photoPrismPhotosService = get(),
                // Always 80 elements – not great, not terrible.
                // It is better, of course, to dynamically adjust
                // to the max number of items on the screen.
                defaultPageLimit = 80,
            )
        } bind SimpleGalleryMediaRepository.Factory::class

        scoped {
            ThreadPoolBackgroundMediaFileDownloadManager(
                downloadFileUseCase = get(),
                downloadUrlFactory = get(),
                poolSize = 6,
            )
        } bind BackgroundMediaFileDownloadManager::class

        viewModelOf(::GallerySearchViewModel)

        viewModelOf(::GalleryFastScrollViewModel)

        viewModelOf(::GalleryListViewModelImpl) bind GalleryListViewModel::class

        viewModel {
            GalleryMediaDownloadActionsViewModelDelegateImpl(
                internalDownloadsDir = get(named(INTERNAL_DOWNLOADS_DIRECTORY)),
                externalDownloadsDir = get(named(EXTERNAL_DOWNLOADS_DIRECTORY)),
                backgroundMediaFileDownloadManager = get(),
                downloadFileUseCase = get(),
                downloadUrlFactory = get(),
                galleryPreferences = get(),
            )
        } bind GalleryMediaDownloadActionsViewModelDelegate::class

        viewModelOf(::GalleryMediaRemoteActionsViewModelDelegateImpl) bind GalleryMediaRemoteActionsViewModelDelegate::class

        viewModel {
            GalleryViewModelGallery(
                galleryMediaRepositoryFactory = get(),
                connectionParams = get<EnvSession>().envConnectionParams,
                searchViewModel = get(),
                fastScrollViewModel = get(),
                disconnectFromEnvUseCase = get(),
                memoriesListViewModel = get(),
                listViewModel = get(),
                galleryMediaDownloadActionsViewModel = get(),
                galleryMediaRemoteActionsViewModel = get(),
            )
        }

        viewModelOf(::GallerySingleRepositoryViewModelGallery)

        scopedOf(::ArchiveGalleryMediaUseCase)
        scopedOf(::DeleteGalleryMediaUseCase)
        scopedOf(::AddGalleryMediaToAlbumUseCase)
        scopedOf(::RemoveGalleryMediaFromAlbumUseCase)
    }
}
