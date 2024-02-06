package ua.com.radiokot.photoprism.features.featureflags.logic

import ua.com.radiokot.photoprism.BuildConfig

class DebugOnlyFeatureFlags: FeatureFlags {
    override val hasMemoriesFeature: Boolean
        get() = BuildConfig.DEBUG
}
