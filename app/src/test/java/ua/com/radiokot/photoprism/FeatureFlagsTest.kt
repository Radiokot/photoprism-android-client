package ua.com.radiokot.photoprism

import org.junit.Assert
import org.junit.Test
import ua.com.radiokot.photoprism.featureflags.logic.FeatureFlags

class FeatureFlagsTest {
    @Test
    fun concatenateSuccessfully() {
        val a = object : FeatureFlags {
            override val hasExtensionPreferences = true
            override val hasMemoriesExtension: Boolean = true
        }
        val b = object : FeatureFlags {
            override val hasExtensionPreferences = false
            override val hasMemoriesExtension: Boolean = false
        }

        Assert.assertTrue((a + b).hasMemoriesExtension)
        Assert.assertTrue((b + a).hasMemoriesExtension)
        Assert.assertFalse((b + b).hasMemoriesExtension)
        Assert.assertTrue((a + a).hasMemoriesExtension)
    }
}
