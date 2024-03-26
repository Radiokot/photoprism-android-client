package ua.com.radiokot.photoprism.featureflags.logic

class FeatureSetFeatureFlags(
    private val featureSet: Set<FeatureFlags.Feature>,
) : FeatureFlags {
    constructor(vararg features: FeatureFlags.Feature): this(features.toSet())

    override fun hasFeature(feature: FeatureFlags.Feature): Boolean =
        feature in featureSet
}
