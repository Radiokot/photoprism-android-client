package ua.com.radiokot.photoprism.features.viewer.di

import android.media.MediaScannerConnection
import okhttp3.Cache
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.module.Module
import org.koin.core.qualifier._q
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module
import ua.com.radiokot.photoprism.di.*
import ua.com.radiokot.photoprism.env.data.model.EnvSession
import ua.com.radiokot.photoprism.features.gallery.di.galleryFeatureModules
import ua.com.radiokot.photoprism.features.viewer.view.DefaultVideoPlayerFactory
import ua.com.radiokot.photoprism.features.viewer.view.VideoPlayerFactory
import ua.com.radiokot.photoprism.features.viewer.view.model.MediaViewerViewModel
import ua.com.radiokot.photoprism.features.viewer.view.model.VideoPlayerCacheViewModel
import ua.com.radiokot.photoprism.util.MediaCacheUtil
import java.io.File

val mediaViewerFeatureModules: List<Module> = listOf(
    module {
        includes(galleryFeatureModules)

        scope<EnvSession> {
            viewModel {
                MediaViewerViewModel(
                    galleryMediaRepositoryFactory = get(),
                    internalDownloadsDir = get(named(INTERNAL_DOWNLOADS_DIRECTORY)),
                    externalDownloadsDir = get(named(EXTERNAL_DOWNLOADS_DIRECTORY))
                ) { path, mimeType ->
                    MediaScannerConnection.scanFile(
                        get(),
                        arrayOf(path),
                        arrayOf(mimeType),
                        null,
                    )
                }
            }

            scoped {
                val session = get<EnvSession>()

                val cacheDir: File = get(named(VIDEO_CACHE_DIRECTORY))
                val httpClient = get<HttpClient>(_q<EnvHttpClientParams>()) {
                    EnvHttpClientParams(
                        sessionAwareness = null,
                        clientCertificateAlias = session.envConnectionParams.clientCertificateAlias,
                        withLogging = false,
                        cache = Cache(cacheDir, MediaCacheUtil.calculateCacheMaxSize(cacheDir))
                    )
                }

                DefaultVideoPlayerFactory(
                    httpClient = httpClient,
                    context = get(),
                )
            } bind VideoPlayerFactory::class

            viewModel {
                VideoPlayerCacheViewModel(
                    videoPlayerFactory = get(),
                    // 2 is enough for pages swiping.
                    cacheMaxSize = 2,
                )
            }
        }
    }
)