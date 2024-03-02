package ua.com.radiokot.photoprism

import org.junit.Assert
import org.junit.Test
import ua.com.radiokot.photoprism.featureflags.logic.FeatureFlags

class FeatureFlagsTest {
    @Test
    fun concatenateSuccessfully() {
        val a = object : FeatureFlags {
            override val hasMemoriesFeature: Boolean = true
        }
        val b = object : FeatureFlags {
            override val hasMemoriesFeature: Boolean = false
        }

        Assert.assertTrue((a + b).hasMemoriesFeature)
        Assert.assertTrue((b + a).hasMemoriesFeature)
        Assert.assertFalse((b + b).hasMemoriesFeature)
        Assert.assertTrue((a + a).hasMemoriesFeature)
    }
}
