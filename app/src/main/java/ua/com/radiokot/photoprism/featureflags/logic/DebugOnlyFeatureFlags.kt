package ua.com.radiokot.photoprism.featureflags.logic

import ua.com.radiokot.photoprism.BuildConfig

class DebugOnlyFeatureFlags: FeatureFlags {
    override val hasExtensionPreferences: Boolean
        get() = BuildConfig.DEBUG

    override val hasMemoriesExtension: Boolean
        get() = BuildConfig.DEBUG
}
