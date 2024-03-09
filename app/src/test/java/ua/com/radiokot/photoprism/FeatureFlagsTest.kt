package ua.com.radiokot.photoprism

import org.junit.Assert
import org.junit.Test
import ua.com.radiokot.photoprism.featureflags.logic.FeatureFlags

class FeatureFlagsTest {
    @Test
    fun concatenateSuccessfully() {
        val a = object : FeatureFlags {
            override fun hasFeature(feature: FeatureFlags.Feature): Boolean = true
        }
        val b = object : FeatureFlags {
            override fun hasFeature(feature: FeatureFlags.Feature): Boolean = false
        }

        Assert.assertTrue((a + b).hasFeature(FeatureFlags.Feature.values().first()))
        Assert.assertTrue((b + a).hasFeature(FeatureFlags.Feature.values().first()))
        Assert.assertFalse((b + b).hasFeature(FeatureFlags.Feature.values().first()))
        Assert.assertTrue((a + a).hasFeature(FeatureFlags.Feature.values().first()))
    }
}
