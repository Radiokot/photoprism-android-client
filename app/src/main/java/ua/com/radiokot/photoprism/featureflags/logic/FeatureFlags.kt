package ua.com.radiokot.photoprism.featureflags.logic

interface FeatureFlags {
    val hasExtensionPreferences: Boolean
    val hasMemoriesExtension: Boolean

    operator fun plus(other: FeatureFlags) = object : FeatureFlags{
        override val hasExtensionPreferences: Boolean
            get() = this@FeatureFlags.hasExtensionPreferences || other.hasExtensionPreferences

        override val hasMemoriesExtension: Boolean
            get() = this@FeatureFlags.hasMemoriesExtension || other.hasMemoriesExtension
    }
}
