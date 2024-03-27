package ua.com.radiokot.photoprism.featureflags.di

import org.koin.dsl.bind
import org.koin.dsl.module
import ua.com.radiokot.photoprism.featureflags.logic.FeatureFlags
import ua.com.radiokot.photoprism.featureflags.logic.FeatureSetFeatureFlags
import ua.com.radiokot.photoprism.features.ext.data.storage.GalleryExtensionsStateRepository

val featureFlagsModule = module {
    single {
        FeatureSetFeatureFlags(
            FeatureFlags.Feature.EXTENSION_PREFERENCES,
        ) + get<GalleryExtensionsStateRepository>()
    } bind FeatureFlags::class
}

val allDisabledFeatureFlagsModule = module {
    single {
        FeatureSetFeatureFlags()
    } bind FeatureFlags::class
}
