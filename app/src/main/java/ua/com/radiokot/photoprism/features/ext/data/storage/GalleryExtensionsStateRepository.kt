package ua.com.radiokot.photoprism.features.ext.data.storage

import ua.com.radiokot.photoprism.base.data.storage.ObjectPersistence
import ua.com.radiokot.photoprism.featureflags.logic.FeatureFlags
import ua.com.radiokot.photoprism.features.ext.data.model.ActivatedGalleryExtension
import ua.com.radiokot.photoprism.features.ext.data.model.GalleryExtensionsState
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
}
