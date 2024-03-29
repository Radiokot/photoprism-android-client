package ua.com.radiokot.photoprism

import org.junit.Assert
import org.junit.Test
import ua.com.radiokot.photoprism.base.data.storage.ObjectPersistence
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

        Assert.assertTrue(repository.hasFeature(GalleryExtension.MEMORIES.feature))
        Assert.assertFalse(repository.hasFeature(GalleryExtension.TEST.feature))
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

        Assert.assertFalse(repository.hasFeature(GalleryExtension.TEST.feature))
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

        Assert.assertFalse(repository.hasFeature(GalleryExtension.TEST.feature))
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

    @Test
    fun activateKeyExtensionsSuccessfully_IfAllNew() {
        val persistence = SimpleStatePersistence(
            state = null
        )
        val repository = GalleryExtensionsStateRepository(
            statePersistence = persistence,
        )

        val keySubject = "test@local"
        val keyExtensions = setOf(GalleryExtension.TEST)
        val encodedKey = "ek"

        val activatedExtensions = repository.activateKeyExtensions(
            keySubject = keySubject,
            keyExtensions = keyExtensions,
            encodedKey = encodedKey,
            keyExpiresAt = null,
        )

        Assert.assertEquals(1, activatedExtensions.size)
        Assert.assertEquals(keyExtensions.first(), activatedExtensions.first().type)
        Assert.assertEquals(keySubject, repository.currentState.primarySubject)
        Assert.assertEquals(encodedKey, activatedExtensions.first().key)
        Assert.assertNull(activatedExtensions.first().expiresAt)
        Assert.assertArrayEquals(activatedExtensions.toTypedArray(), repository.currentState.activatedExtensions.toTypedArray())
        Assert.assertTrue(repository.hasFeature(GalleryExtension.TEST.feature))
    }

    @Test
    fun activateKeyExtensionsSuccessfully_IfSomeNew() {
        val persistence = SimpleStatePersistence(
            state = GalleryExtensionsState(
                activatedExtensions = listOf(
                    ActivatedGalleryExtension(
                        type = GalleryExtension.MEMORIES,
                        key = "keyA",
                        expiresAt = null,
                    ),
                ),
                primarySubject = "test@local"
            )
        )
        val repository = GalleryExtensionsStateRepository(
            statePersistence = persistence,
        )

        val keySubject = "test@local"
        val keyExtensions = setOf(
            GalleryExtension.TEST,
            GalleryExtension.MEMORIES
        )
        val encodedKey = "ek"

        val activatedExtensions = repository.activateKeyExtensions(
            keySubject = keySubject,
            keyExtensions = keyExtensions,
            encodedKey = encodedKey,
            keyExpiresAt = null,
        )

        Assert.assertEquals(1, activatedExtensions.size)
        Assert.assertEquals(GalleryExtension.TEST, activatedExtensions.first().type)
        Assert.assertEquals(keySubject, repository.currentState.primarySubject)
        Assert.assertEquals(encodedKey, activatedExtensions.first().key)
        Assert.assertEquals("keyA", repository.currentState.activatedExtensions.first().key)
        Assert.assertTrue(repository.hasFeature(GalleryExtension.TEST.feature))
    }

    @Test
    fun activateNoKeyExtensions_IfNoneNew() {
        val persistence = SimpleStatePersistence(
            state = GalleryExtensionsState(
                activatedExtensions = listOf(
                    ActivatedGalleryExtension(
                        type = GalleryExtension.MEMORIES,
                        key = "keyA",
                        expiresAt = null,
                    ),
                ),
                primarySubject = "test@local"
            )
        )
        val repository = GalleryExtensionsStateRepository(
            statePersistence = persistence,
        )

        val keySubject = "test@local"
        val keyExtensions = setOf(
            GalleryExtension.MEMORIES
        )
        val encodedKey = "ek"

        val activatedExtensions = repository.activateKeyExtensions(
            keySubject = keySubject,
            keyExtensions = keyExtensions,
            encodedKey = encodedKey,
            keyExpiresAt = null,
        )

        Assert.assertEquals(0, activatedExtensions.size)
        Assert.assertEquals("keyA", repository.currentState.activatedExtensions.first().key)
    }

    @Test
    fun activateKeyExtensionsSuccessfully_IfProlongingAlreadyActivated() {
        val persistence = SimpleStatePersistence(
            state = GalleryExtensionsState(
                activatedExtensions = listOf(
                    ActivatedGalleryExtension(
                        type = GalleryExtension.MEMORIES,
                        key = "keyA",
                        expiresAt = Date(System.currentTimeMillis() + 10000),
                    ),
                    ActivatedGalleryExtension(
                        type = GalleryExtension.TEST,
                        key = "keyB",
                        expiresAt = Date(System.currentTimeMillis() + 6000),
                    ),
                ),
                primarySubject = "test@local"
            )
        )
        val repository = GalleryExtensionsStateRepository(
            statePersistence = persistence,
        )

        val keySubject = "test@local"
        val keyExtensions = setOf(
            GalleryExtension.TEST,
            GalleryExtension.MEMORIES
        )
        val encodedKey = "ek"
        val keyExpirationDate = Date(System.currentTimeMillis() + 30000)

        val activatedExtensions = repository.activateKeyExtensions(
            keySubject = keySubject,
            keyExtensions = keyExtensions,
            encodedKey = encodedKey,
            keyExpiresAt = keyExpirationDate,
        )

        Assert.assertEquals(2, activatedExtensions.size)
        Assert.assertEquals(encodedKey, repository.currentState.activatedExtensions[0].key)
        Assert.assertEquals(keyExpirationDate, repository.currentState.activatedExtensions[0].expiresAt)
        Assert.assertEquals(encodedKey, repository.currentState.activatedExtensions[1].key)
        Assert.assertEquals(keyExpirationDate, repository.currentState.activatedExtensions[1].expiresAt)
    }

    @Test
    fun activateKeyExtensionsSuccessfully_IfProlongingAlreadyActivatedForever() {
        val persistence = SimpleStatePersistence(
            state = GalleryExtensionsState(
                activatedExtensions = listOf(
                    ActivatedGalleryExtension(
                        type = GalleryExtension.MEMORIES,
                        key = "keyA",
                        expiresAt = Date(System.currentTimeMillis() + 10000),
                    ),
                    ActivatedGalleryExtension(
                        type = GalleryExtension.TEST,
                        key = "keyB",
                        expiresAt = Date(System.currentTimeMillis() + 6000),
                    ),
                ),
                primarySubject = "test@local"
            )
        )
        val repository = GalleryExtensionsStateRepository(
            statePersistence = persistence,
        )

        val keySubject = "test@local"
        val keyExtensions = setOf(
            GalleryExtension.TEST,
            GalleryExtension.MEMORIES
        )
        val encodedKey = "ek"

        val activatedExtensions = repository.activateKeyExtensions(
            keySubject = keySubject,
            keyExtensions = keyExtensions,
            encodedKey = encodedKey,
            keyExpiresAt = null,
        )

        Assert.assertEquals(2, activatedExtensions.size)
        Assert.assertEquals(encodedKey, repository.currentState.activatedExtensions[0].key)
        Assert.assertNull(repository.currentState.activatedExtensions[0].expiresAt)
        Assert.assertEquals(encodedKey, repository.currentState.activatedExtensions[1].key)
        Assert.assertNull(repository.currentState.activatedExtensions[1].expiresAt)
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
