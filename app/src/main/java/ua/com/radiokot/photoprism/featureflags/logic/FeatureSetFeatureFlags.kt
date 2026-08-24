package ua.com.radiokot.photoprism.featureflags.logic

class FeatureSetFeatureFlags(
    private val featureSet: Set<FeatureFlags.Feature>,
) : FeatureFlags {

    constructor(
        builderAction: MutableSet<FeatureFlags.Feature>.() -> Unit
    ) : this(buildSet(builderAction))

    override fun hasFeature(feature: FeatureFlags.Feature): Boolean =
        feature in featureSet
}
