package ua.com.radiokot.photoprism.features.viewer

import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import org.koin.androidx.viewmodel.dsl.viewModelOf
import org.koin.core.module.dsl.scopedOf
import org.koin.core.qualifier._q
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module
import ua.com.radiokot.photoprism.di.EnvHttpClientParams
import ua.com.radiokot.photoprism.di.HttpClient
import ua.com.radiokot.photoprism.di.VIDEO_CACHE_DIRECTORY
import ua.com.radiokot.photoprism.env.data.model.EnvSession
import ua.com.radiokot.photoprism.features.gallery.di.galleryFeatureModule
import ua.com.radiokot.photoprism.features.viewer.logic.DefaultVideoPlayerFactory
import ua.com.radiokot.photoprism.features.viewer.logic.UpdateGalleryMediaAttributesUseCase
import ua.com.radiokot.photoprism.features.viewer.logic.VideoPlayerFactory
import ua.com.radiokot.photoprism.features.viewer.view.model.MediaViewerViewModel
import ua.com.radiokot.photoprism.features.viewer.view.model.VideoPlayerCacheViewModel
import ua.com.radiokot.photoprism.util.CacheConstraints
import java.io.File

@OptIn(UnstableApi::class)
val mediaViewerFeatureModule = module {
    includes(galleryFeatureModule)

    // Shared ExoPlayer cache.
    // Must be single instance per directory.
    single {
        val cacheDir: File = get(named(VIDEO_CACHE_DIRECTORY))
        SimpleCache(
            cacheDir,
            LeastRecentlyUsedCacheEvictor(
                CacheConstraints.getOptimalSize(cacheDir)
            ),
            StandaloneDatabaseProvider(get())
        )
    } bind Cache::class

    scope<EnvSession> {
        viewModelOf(::MediaViewerViewModel)

        scoped {
            val session = get<EnvSession>()

            // Own HTTP client is used for video player to enable mTLS and HTTP basic auth.
            // It should not have a cache, as it is managed by the player.
            val httpClient = get<HttpClient>(_q<EnvHttpClientParams>()) {
                EnvHttpClientParams(
                    sessionAwareness = null,
                    clientCertificateAlias = session.envConnectionParams.clientCertificateAlias,
                    authorization = session.envConnectionParams.httpAuth,
                    withLogging = false,
                )
            }

            DefaultVideoPlayerFactory(
                httpClient = httpClient,
                sharedCache = get(),
                context = get(),
            )
        } bind VideoPlayerFactory::class

        viewModelOf(::VideoPlayerCacheViewModel)

        scopedOf(::UpdateGalleryMediaAttributesUseCase)
    }
}
