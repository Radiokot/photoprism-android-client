package ua.com.radiokot.photoprism.featureflags.logic

import kotlin.experimental.ExperimentalTypeInference

class FeatureSetFeatureFlags(
    private val featureSet: Set<FeatureFlags.Feature>,
) : FeatureFlags {

    @OptIn(ExperimentalTypeInference::class)
    constructor(
        @BuilderInference
        builderAction: MutableSet<FeatureFlags.Feature>.() -> Unit
    ) : this(buildSet(builderAction))

    override fun hasFeature(feature: FeatureFlags.Feature): Boolean =
        feature in featureSet
}
