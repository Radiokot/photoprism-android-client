package ua.com.radiokot.photoprism.featureflags.logic

import ua.com.radiokot.photoprism.BuildConfig

class DebugOnlyFeatureFlags : FeatureFlags {
    override fun hasFeature(feature: FeatureFlags.Feature): Boolean = BuildConfig.DEBUG
}
