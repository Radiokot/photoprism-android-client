package ua.com.radiokot.photoprism.features.viewer.di

import android.media.MediaScannerConnection
import android.os.Environment
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module
import ua.com.radiokot.photoprism.env.data.model.EnvSession
import ua.com.radiokot.photoprism.features.gallery.di.INTERNAL_DOWNLOADS_DIRECTORY
import ua.com.radiokot.photoprism.features.gallery.di.galleryFeatureModules
import ua.com.radiokot.photoprism.features.viewer.logic.WrappedMediaScannerConnection
import ua.com.radiokot.photoprism.features.viewer.view.model.VideoPlayerCacheViewModel
import ua.com.radiokot.photoprism.features.viewer.view.model.MediaViewerViewModel
import java.io.File

const val EXTERNAL_DOWNLOADS_DIRECTORY = "external-downloads"

val mediaViewerFeatureModules: List<Module> = listOf(
    module {
        includes(galleryFeatureModules)

        single(named(EXTERNAL_DOWNLOADS_DIRECTORY)) {
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                .apply { mkdirs() }
        } bind File::class

        scope<EnvSession> {
            viewModel {
                MediaViewerViewModel(
                    galleryMediaRepositoryFactory = get(),
                    internalDownloadsDir = get(named(INTERNAL_DOWNLOADS_DIRECTORY)),
                    externalDownloadsDir = get(named(EXTERNAL_DOWNLOADS_DIRECTORY)),
                    mediaScannerConnection = object : WrappedMediaScannerConnection {
                        override fun scanFile(path: String, mimeType: String) {
                            MediaScannerConnection.scanFile(
                                get(),
                                arrayOf(path),
                                arrayOf(mimeType),
                                null,
                            )
                        }
                    }
                )
            }

            viewModel {
                VideoPlayerCacheViewModel()
            }
        }
    }
)