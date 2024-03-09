package ua.com.radiokot.photoprism.featureflags.extension

import ua.com.radiokot.photoprism.featureflags.logic.FeatureFlags

val FeatureFlags.hasMemoriesExtension: Boolean
    get() = hasFeature(FeatureFlags.Feature.MEMORIES_EXTENSION)

val FeatureFlags.hasExtensionPreferences: Boolean
    get() = hasFeature(FeatureFlags.Feature.EXTENSION_PREFERENCES)
