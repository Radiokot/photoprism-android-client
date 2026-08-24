package ua.com.radiokot.photoprism.featureflags.di

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import org.koin.android.ext.koin.androidApplication
import org.koin.dsl.bind
import org.koin.dsl.module
import ua.com.radiokot.photoprism.featureflags.logic.FeatureFlags
import ua.com.radiokot.photoprism.featureflags.logic.FeatureSetFeatureFlags
import ua.com.radiokot.photoprism.features.ext.data.storage.GalleryExtensionsStateRepository

val devFeatureFlagsModule = module {
    single {
        FeatureSetFeatureFlags {
            add(FeatureFlags.Feature.PHOTO_FRAME_WIDGET)
            add(FeatureFlags.Feature.EXTENSION_STORE)
            addMapIfDeviceSupports(
                context = androidApplication(),
            )
        } + get<GalleryExtensionsStateRepository>()
    } bind FeatureFlags::class
}

val releaseFeatureFlagsModule = module {
    single {
        FeatureSetFeatureFlags {
            add(FeatureFlags.Feature.EXTENSION_STORE)
            addMapIfDeviceSupports(
                context = androidApplication(),
            )
        } + get<GalleryExtensionsStateRepository>()
    } bind FeatureFlags::class
}

val playReleaseFeatureFlagsModule = module {
    single {
        FeatureSetFeatureFlags {
            addMapIfDeviceSupports(
                context = androidApplication(),
            )
        } + get<GalleryExtensionsStateRepository>()
    } bind FeatureFlags::class
}

private fun MutableSet<FeatureFlags.Feature>.addMapIfDeviceSupports(
    context: Context,
) {
    // MapLibre minSDK is 23, but it also requires Vulkan.
    if (Build.VERSION.SDK_INT < 24) {
        return
    }

    val pm = context.packageManager
    // github.com/maplibre/maplibre-native/blob/main/platform/android/MapLibreAndroid/src/vulkan/AndroidManifest.xml
    val requiredVulkanVersion = 0x400003

    if (!pm.hasSystemFeature(
            PackageManager.FEATURE_VULKAN_HARDWARE_VERSION,
            requiredVulkanVersion
        )
    ) {
        return
    }

    add(FeatureFlags.Feature.MAP)
}
