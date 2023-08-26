package ua.com.radiokot.photoprism.features.viewer.di

import com.google.android.exoplayer2.database.StandaloneDatabaseProvider
import com.google.android.exoplayer2.upstream.cache.Cache
import com.google.android.exoplayer2.upstream.cache.LeastRecentlyUsedCacheEvictor
import com.google.android.exoplayer2.upstream.cache.SimpleCache
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.module.Module
import org.koin.core.qualifier._q
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module
import ua.com.radiokot.photoprism.di.EXTERNAL_DOWNLOADS_DIRECTORY
import ua.com.radiokot.photoprism.di.EnvHttpClientParams
import ua.com.radiokot.photoprism.di.HttpClient
import ua.com.radiokot.photoprism.di.INTERNAL_DOWNLOADS_DIRECTORY
import ua.com.radiokot.photoprism.di.UTC_DATE_TIME_DATE_FORMAT
import ua.com.radiokot.photoprism.di.UTC_DATE_TIME_YEAR_DATE_FORMAT
import ua.com.radiokot.photoprism.di.VIDEO_CACHE_DIRECTORY
import ua.com.radiokot.photoprism.env.data.model.EnvSession
import ua.com.radiokot.photoprism.features.gallery.di.galleryFeatureModules
import ua.com.radiokot.photoprism.features.viewer.logic.BackgroundMediaFileDownloadManager
import ua.com.radiokot.photoprism.features.viewer.logic.ThreadPoolBackgroundMediaFileDownloadManager
import ua.com.radiokot.photoprism.features.viewer.view.DefaultVideoPlayerFactory
import ua.com.radiokot.photoprism.features.viewer.view.VideoPlayerFactory
import ua.com.radiokot.photoprism.features.viewer.view.model.MediaViewerViewModel
import ua.com.radiokot.photoprism.features.viewer.view.model.VideoPlayerCacheViewModel
import ua.com.radiokot.photoprism.util.MediaCacheUtil
import java.io.File

val mediaViewerFeatureModules: List<Module> = listOf(
    module {
        includes(galleryFeatureModules)

        // Shared ExoPlayer cache.
        // Must be single instance per directory.
        single {
            val cacheDir: File = get(named(VIDEO_CACHE_DIRECTORY))
            SimpleCache(
                cacheDir,
                LeastRecentlyUsedCacheEvictor(
                    MediaCacheUtil.calculateCacheMaxSize(cacheDir)
                ),
                StandaloneDatabaseProvider(get())
            )
        } bind Cache::class

        scope<EnvSession> {
            scoped {
                ThreadPoolBackgroundMediaFileDownloadManager(
                    downloadFileUseCaseFactory = get(),
                    poolSize = 6,
                )
            } bind BackgroundMediaFileDownloadManager::class

            viewModel {
                MediaViewerViewModel(
                    galleryMediaRepositoryFactory = get(),
                    internalDownloadsDir = get(named(INTERNAL_DOWNLOADS_DIRECTORY)),
                    externalDownloadsDir = get(named(EXTERNAL_DOWNLOADS_DIRECTORY)),
                    downloadMediaFileViewModel = get(),
                    backgroundMediaFileDownloadManager = get(),
                    utcDateTimeDateFormat = get(named(UTC_DATE_TIME_DATE_FORMAT)),
                    utcDateTimeYearDateFormat = get(named(UTC_DATE_TIME_YEAR_DATE_FORMAT)),
                )
            }

            scoped {
                val session = get<EnvSession>()

                // Own HTTP client is used for video player to enable mTLS and HTTP basic auth.
                // It should not have a cache, as it is managed by the player.
                val httpClient = get<HttpClient>(_q<EnvHttpClientParams>()) {
                    EnvHttpClientParams(
                        sessionAwareness = null,
                        clientCertificateAlias = session.envConnectionParams.clientCertificateAlias,
                        withLogging = false,
                    )
                }

                DefaultVideoPlayerFactory(
                    httpClient = httpClient,
                    sharedCache = get(),
                    context = get(),
                )
            } bind VideoPlayerFactory::class

            viewModel {
                VideoPlayerCacheViewModel(
                    videoPlayerFactory = get(),
                )
            }
        }
    }
)
