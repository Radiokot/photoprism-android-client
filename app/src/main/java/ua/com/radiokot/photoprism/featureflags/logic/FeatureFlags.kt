package ua.com.radiokot.photoprism.featureflags.logic

interface FeatureFlags {
    fun hasFeature(feature: Feature): Boolean

    operator fun contains(feature: Feature): Boolean =
        hasFeature(feature)

    operator fun plus(other: FeatureFlags) = object : FeatureFlags{
        override fun hasFeature(feature: Feature): Boolean =
            feature in this@FeatureFlags || feature in other
    }

    enum class Feature {
        EXTENSION_STORE,
        PHOTO_FRAME_WIDGET,
        MAP,
        MEMORIES_EXTENSION,
        TEST_EXTENSION,
        ;
    }
}
