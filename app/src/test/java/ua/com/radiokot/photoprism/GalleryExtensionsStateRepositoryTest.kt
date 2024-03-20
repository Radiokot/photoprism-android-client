package ua.com.radiokot.photoprism

import org.junit.Assert
import org.junit.Test
import ua.com.radiokot.photoprism.base.data.storage.ObjectPersistence
import ua.com.radiokot.photoprism.featureflags.logic.FeatureFlags
import ua.com.radiokot.photoprism.features.ext.data.model.ActivatedGalleryExtension
import ua.com.radiokot.photoprism.features.ext.data.model.GalleryExtension
import ua.com.radiokot.photoprism.features.ext.data.model.GalleryExtensionsState
import ua.com.radiokot.photoprism.features.ext.data.storage.GalleryExtensionsStateRepository
import java.util.Date

class GalleryExtensionsStateRepositoryTest {
    @Test
    fun featureFlagsWork() {
        val persistence = SimpleStatePersistence(
            state = GalleryExtensionsState(
                activatedExtensions = listOf(
                    ActivatedGalleryExtension(
                        type = GalleryExtension.MEMORIES,
                        key = "key",
                        expiresAt = null,
                    )
                ),
                primarySubject = "test@local"
            )
        )
        val repository = GalleryExtensionsStateRepository(
            statePersistence = persistence,
        )

        Assert.assertTrue(repository.hasFeature(FeatureFlags.Feature.MEMORIES_EXTENSION))
        Assert.assertFalse(repository.hasFeature(FeatureFlags.Feature.TEST_EXTENSION))
    }

    @Test
    fun expiredExtensionsRemoved() {
        val persistence = SimpleStatePersistence(
            state = GalleryExtensionsState(
                activatedExtensions = listOf(
                    ActivatedGalleryExtension(
                        type = GalleryExtension.MEMORIES,
                        key = "key",
                        expiresAt = null,
                    ),
                    ActivatedGalleryExtension(
                        type = GalleryExtension.TEST,
                        key = "key",
                        expiresAt = Date(System.currentTimeMillis() - 1000)
                    )
                ),
                primarySubject = "test@local"
            )
        )
        val repository = GalleryExtensionsStateRepository(
            statePersistence = persistence,
        )

        Assert.assertFalse(repository.hasFeature(FeatureFlags.Feature.TEST_EXTENSION))
        Assert.assertEquals(1, persistence.loadItem()!!.activatedExtensions.size)
        Assert.assertEquals(
            GalleryExtension.MEMORIES,
            persistence.loadItem()!!.activatedExtensions.first().type
        )
        Assert.assertEquals("test@local", persistence.loadItem()!!.primarySubject)
    }

    @Test
    fun primarySubjectErased_IfAllExtensionsExpired() {
        val persistence = SimpleStatePersistence(
            state = GalleryExtensionsState(
                activatedExtensions = listOf(
                    ActivatedGalleryExtension(
                        type = GalleryExtension.TEST,
                        key = "key",
                        expiresAt = Date(System.currentTimeMillis() - 1000)
                    )
                ),
                primarySubject = "test@local"
            )
        )
        val repository = GalleryExtensionsStateRepository(
            statePersistence = persistence,
        )

        Assert.assertFalse(repository.hasFeature(FeatureFlags.Feature.TEST_EXTENSION))
        Assert.assertNull(persistence.loadItem()!!.primarySubject)
    }

    @Test
    fun activatedKeysCollectedWithoutDuplicates() {
        val state = GalleryExtensionsState(
            activatedExtensions = listOf(
                ActivatedGalleryExtension(
                    type = GalleryExtension.MEMORIES,
                    key = "keyA",
                    expiresAt = null,
                ),
                ActivatedGalleryExtension(
                    type = GalleryExtension.TEST,
                    key = "keyA",
                    expiresAt = null,
                )
            ),
            primarySubject = "test@local"
        )

        Assert.assertEquals(setOf("keyA"), state.activatedKeys)
    }

    private class SimpleStatePersistence(
        private var state: GalleryExtensionsState?,
    ) : ObjectPersistence<GalleryExtensionsState> {
        override fun loadItem(): GalleryExtensionsState? = state
        override fun hasItem(): Boolean = state != null

        override fun clear() {
            state = null
        }

        override fun saveItem(item: GalleryExtensionsState) {
            state = item
        }
    }
}
