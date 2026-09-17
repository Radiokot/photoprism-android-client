package ua.com.radiokot.photoprism.featureflags.di

import android.app.ActivityManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import org.koin.android.ext.koin.androidApplication
import org.koin.dsl.bind
import org.koin.dsl.module
import ua.com.radiokot.photoprism.featureflags.logic.FeatureFlags
import ua.com.radiokot.photoprism.featureflags.logic.FeatureSetFeatureFlags
import ua.com.radiokot.photoprism.features.ext.data.storage.GalleryExtensionsStateRepository
import ua.com.radiokot.photoprism.features.gallery.search.logic.TvDetector

val devFeatureFlagsModule = module {
    single {
        FeatureSetFeatureFlags {
            add(FeatureFlags.Feature.PHOTO_FRAME_WIDGET)
            add(FeatureFlags.Feature.EXTENSION_STORE)
            addMapIfSupported(
                context = androidApplication(),
            )
            addPanorama3DViewerIfSupported(
                context = androidApplication(),
                tvDetector = get(),
            )
        } + get<GalleryExtensionsStateRepository>()
    } bind FeatureFlags::class
}

val releaseFeatureFlagsModule = module {
    single {
        FeatureSetFeatureFlags {
            add(FeatureFlags.Feature.EXTENSION_STORE)
            addMapIfSupported(
                context = androidApplication(),
            )
            addPanorama3DViewerIfSupported(
                context = androidApplication(),
                tvDetector = get(),
            )
        } + get<GalleryExtensionsStateRepository>()
    } bind FeatureFlags::class
}

val playReleaseFeatureFlagsModule = module {
    single {
        FeatureSetFeatureFlags {
            addMapIfSupported(
                context = androidApplication(),
            )
            addPanorama3DViewerIfSupported(
                context = androidApplication(),
                tvDetector = get(),
            )
        } + get<GalleryExtensionsStateRepository>()
    } bind FeatureFlags::class
}

private fun MutableSet<FeatureFlags.Feature>.addMapIfSupported(
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

private fun MutableSet<FeatureFlags.Feature>.addPanorama3DViewerIfSupported(
    context: Context,
    tvDetector: TvDetector,
) {
    if (Build.VERSION.SDK_INT < 23
        || tvDetector.isRunningOnTv
    ) {
        return
    }

    val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager

    // Viewer needs ES 2.0 or higher.
    if (am.deviceConfigurationInfo.reqGlEsVersion < 0x00020000) {
        return
    }

    add(FeatureFlags.Feature.PANORAMA_3D_VIEWER)
}
