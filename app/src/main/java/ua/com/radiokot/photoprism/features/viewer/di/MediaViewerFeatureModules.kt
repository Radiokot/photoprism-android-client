package ua.com.radiokot.photoprism.features.viewer.di

import android.media.MediaScannerConnection
import android.os.Environment
import com.squareup.picasso.PicassoUtilsProxy
import okhttp3.Cache
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.module.Module
import org.koin.core.qualifier._q
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module
import ua.com.radiokot.photoprism.di.EnvHttpClientParams
import ua.com.radiokot.photoprism.di.HttpClient
import ua.com.radiokot.photoprism.env.data.model.EnvSession
import ua.com.radiokot.photoprism.features.gallery.di.INTERNAL_DOWNLOADS_DIRECTORY
import ua.com.radiokot.photoprism.features.gallery.di.galleryFeatureModules
import ua.com.radiokot.photoprism.features.viewer.view.DefaultVideoPlayerFactory
import ua.com.radiokot.photoprism.features.viewer.view.VideoPlayerFactory
import ua.com.radiokot.photoprism.features.viewer.view.model.MediaViewerViewModel
import ua.com.radiokot.photoprism.features.viewer.view.model.VideoPlayerCacheViewModel
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

                // TODO: inject cache dirs: picasso-cache and video-cache
                // TODO: move cache size calculator to some no-Picasso util
                val cacheDir = File(androidContext().cacheDir, "video-cache")
                    .apply { mkdirs() }
                val cacheSize = PicassoUtilsProxy.calculateDiskCacheSize(cacheDir)
                val httpClient = get<HttpClient>(_q<EnvHttpClientParams>()) {
                    EnvHttpClientParams(
                        sessionAwareness = null,
                        clientCertificateAlias = session.envConnectionParams.clientCertificateAlias,
                        withLogging = false,
                    )
                }
                    .newBuilder()
                    .cache(Cache(cacheDir, cacheSize))
                    .build()

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