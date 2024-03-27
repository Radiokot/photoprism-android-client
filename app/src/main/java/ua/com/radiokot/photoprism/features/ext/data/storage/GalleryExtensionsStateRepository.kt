package ua.com.radiokot.photoprism.features.ext.data.storage

import ua.com.radiokot.photoprism.base.data.storage.ObjectPersistence
import ua.com.radiokot.photoprism.featureflags.logic.FeatureFlags
import ua.com.radiokot.photoprism.features.ext.data.model.ActivatedGalleryExtension
import ua.com.radiokot.photoprism.features.ext.data.model.GalleryExtension
import ua.com.radiokot.photoprism.features.ext.data.model.GalleryExtensionsState
import java.util.Date
import kotlin.properties.Delegates

class GalleryExtensionsStateRepository(
    private val statePersistence: ObjectPersistence<GalleryExtensionsState>,
) : FeatureFlags {
    private var _state: GalleryExtensionsState by Delegates.observable(
        initialValue = statePersistence.loadItem() ?: GalleryExtensionsState(),
        onChange = { _, _, newState ->
            statePersistence.saveItem(newState)
        }
    )
    val state: GalleryExtensionsState
        get() = _state

    init {
        val notExpiredExtensions = state.activatedExtensions
            .filterNot(ActivatedGalleryExtension::isExpired)

        if (notExpiredExtensions.size < state.activatedExtensions.size) {
            _state = state.copy(
                activatedExtensions = notExpiredExtensions,
                primarySubject =
                // If all the activated extensions have expired,
                // forget the primary subject.
                if (notExpiredExtensions.isEmpty())
                    null
                else
                    state.primarySubject,
            )
        }
    }

    private val activatedExtensionsFeatures: MutableSet<FeatureFlags.Feature> =
        state.activatedExtensions
            .map { it.type.feature }
            .toMutableSet()

    override fun hasFeature(feature: FeatureFlags.Feature): Boolean =
        feature in activatedExtensionsFeatures

    /**
     * Activates extensions parsed from a key.
     * Which of the [keyExtensions] will actually be activated depends on the current state.
     *
     * @return collection of actually activated extensions,
     * which may be smaller than [keyExtensions].
     */
    fun activateKeyExtensions(
        keySubject: String,
        keyExtensions: Collection<GalleryExtension>,
        keyExpirationDate: Date?,
        encodedKey: String,
    ): Collection<ActivatedGalleryExtension> {
        if (state.primarySubject != null) {
            require(keySubject == state.primarySubject) {
                "The key subject must match the current primary subject: ${state.primarySubject}"
            }
        }

        val alreadyActivatedExtensions = state.activatedExtensions.toMutableList()
        val newActivatedExtensions = mutableListOf<ActivatedGalleryExtension>()

        keyExtensions.forEach { keyExtension ->
            val alreadyActivatedExtension = alreadyActivatedExtensions
                .find { it.type == keyExtension }

            // Activate the extension if it is new (not already activated),
            // or if it prolongs the already activated one.
            if (alreadyActivatedExtension == null
                || alreadyActivatedExtension.expiresAt != null
                && (keyExpirationDate == null || keyExpirationDate > alreadyActivatedExtension.expiresAt)
            ) {
                newActivatedExtensions.add(
                    ActivatedGalleryExtension(
                        type = keyExtension,
                        key = encodedKey,
                        expiresAt = keyExpirationDate,
                    )
                )

                if (alreadyActivatedExtension != null) {
                    alreadyActivatedExtensions.remove(alreadyActivatedExtension)
                }
            }
        }

        _state = state.copy(
            primarySubject = keySubject,
            activatedExtensions = alreadyActivatedExtensions + newActivatedExtensions,
        )

        return newActivatedExtensions
    }
}
