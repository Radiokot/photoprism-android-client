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
            addMapIfDeviceSupports()
        } + get<GalleryExtensionsStateRepository>()
    } bind FeatureFlags::class
}

val releaseFeatureFlagsModule = module {
    single {
        FeatureSetFeatureFlags {
            add(FeatureFlags.Feature.EXTENSION_STORE)
            addMapIfDeviceSupports()
        } + get<GalleryExtensionsStateRepository>()
    } bind FeatureFlags::class
}

val playReleaseFeatureFlagsModule = module {
    single {
        FeatureSetFeatureFlags {
            addMapIfDeviceSupports()
        } + get<GalleryExtensionsStateRepository>()
    } bind FeatureFlags::class
}

private fun MutableSet<FeatureFlags.Feature>.addMapIfDeviceSupports() {
    // MapLibre minSDK is 23, but it also requires Vulkan 1.0.
    if (Build.VERSION.SDK_INT >= 24) {
        add(FeatureFlags.Feature.MAP)
    }
}
