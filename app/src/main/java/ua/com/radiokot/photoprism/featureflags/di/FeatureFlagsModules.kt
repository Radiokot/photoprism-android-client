package ua.com.radiokot.photoprism.featureflags.di

import android.os.Build
import org.koin.dsl.bind
import org.koin.dsl.module
import ua.com.radiokot.photoprism.featureflags.logic.FeatureFlags
import ua.com.radiokot.photoprism.featureflags.logic.FeatureSetFeatureFlags
import ua.com.radiokot.photoprism.features.ext.data.storage.GalleryExtensionsStateRepository

val devFeatureFlagsModule = module {
    single {
        FeatureSetFeatureFlags {
            add(FeatureFlags.Feature.EXTENSION_STORE)
            addMapOnSdk23AndNewer()
        } + get<GalleryExtensionsStateRepository>()
    } bind FeatureFlags::class
}

val releaseFeatureFlagsModule = module {
    single {
        FeatureSetFeatureFlags {
            add(FeatureFlags.Feature.EXTENSION_STORE)
            addMapOnSdk23AndNewer()
        } + get<GalleryExtensionsStateRepository>()
    } bind FeatureFlags::class
}

val playReleaseFeatureFlagsModule = module {
    single {
        FeatureSetFeatureFlags {
            addMapOnSdk23AndNewer()
        } + get<GalleryExtensionsStateRepository>()
    } bind FeatureFlags::class
}

private fun MutableSet<FeatureFlags.Feature>.addMapOnSdk23AndNewer() {
    if (Build.VERSION.SDK_INT >= 23) {
        add(FeatureFlags.Feature.MAP)
    }
}
