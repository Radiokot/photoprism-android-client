package ua.com.radiokot.photoprism.featureflags.extension

import ua.com.radiokot.photoprism.featureflags.logic.FeatureFlags

val FeatureFlags.hasMemoriesExtension: Boolean
    get() = hasFeature(FeatureFlags.Feature.MEMORIES_EXTENSION)

val FeatureFlags.hasExtensionStore: Boolean
    get() = hasFeature(FeatureFlags.Feature.EXTENSION_STORE)

val FeatureFlags.hasPhotoFrameWidget: Boolean
    get() = hasFeature(FeatureFlags.Feature.PHOTO_FRAME_WIDGET)

val FeatureFlags.hasMap: Boolean
    get() = hasFeature(FeatureFlags.Feature.MAP)
