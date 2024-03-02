package ua.com.radiokot.photoprism.featureflags.logic

interface FeatureFlags {
    val hasMemoriesFeature: Boolean

    operator fun plus(other: FeatureFlags) = object : FeatureFlags{
        override val hasMemoriesFeature: Boolean
            get() = this@FeatureFlags.hasMemoriesFeature || other.hasMemoriesFeature
    }
}
