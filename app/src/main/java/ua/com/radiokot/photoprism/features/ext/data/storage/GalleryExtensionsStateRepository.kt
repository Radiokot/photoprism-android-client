package ua.com.radiokot.photoprism.features.ext.data.storage

import android.annotation.SuppressLint
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.subjects.BehaviorSubject
import ua.com.radiokot.photoprism.base.data.storage.ObjectPersistence
import ua.com.radiokot.photoprism.featureflags.logic.FeatureFlags
import ua.com.radiokot.photoprism.features.ext.data.model.ActivatedGalleryExtension
import ua.com.radiokot.photoprism.features.ext.data.model.GalleryExtension
import ua.com.radiokot.photoprism.features.ext.data.model.GalleryExtensionsState
import java.util.Date

@SuppressLint("CheckResult")
class GalleryExtensionsStateRepository(
    private val statePersistence: ObjectPersistence<GalleryExtensionsState>,
) : FeatureFlags {
    private var activatedExtensionsFeatures: Set<FeatureFlags.Feature> = emptySet()

    private val stateSubject =
        BehaviorSubject.createDefault(statePersistence.loadItem() ?: GalleryExtensionsState())
    val state: Observable<GalleryExtensionsState> = stateSubject
    val currentState: GalleryExtensionsState
        get() = stateSubject.value!!

    init {
        state
            .doOnNext { eachState ->
                activatedExtensionsFeatures = eachState.activatedExtensions
                    .mapTo(mutableSetOf()) { it.type.feature }
            }
            .skip(1)
            .subscribe(statePersistence::saveItem)

        val notExpiredExtensions = currentState.activatedExtensions
            .filterNot(ActivatedGalleryExtension::isExpired)

        if (notExpiredExtensions.size < currentState.activatedExtensions.size) {
            stateSubject.onNext(
                currentState.copy(
                    activatedExtensions = notExpiredExtensions,
                    primarySubject =
                    // If all the activated extensions have expired,
                    // forget the primary subject.
                    if (notExpiredExtensions.isEmpty())
                        null
                    else
                        currentState.primarySubject,
                )
            )
        }
    }

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
        keyExpiresAt: Date?,
        encodedKey: String,
    ): Collection<ActivatedGalleryExtension> {
        if (currentState.primarySubject != null) {
            require(keySubject == currentState.primarySubject) {
                "The key subject must match the current primary subject: ${currentState.primarySubject}"
            }
        }

        val alreadyActivatedExtensions = currentState.activatedExtensions.toMutableList()
        val newActivatedExtensions = mutableListOf<ActivatedGalleryExtension>()

        keyExtensions.forEach { keyExtension ->
            val alreadyActivatedExtension = alreadyActivatedExtensions
                .find { it.type == keyExtension }

            // Activate the extension if it is new (not already activated),
            // or if it prolongs the already activated one.
            if (alreadyActivatedExtension == null
                || alreadyActivatedExtension.expiresAt != null
                && (keyExpiresAt == null || keyExpiresAt > alreadyActivatedExtension.expiresAt)
            ) {
                newActivatedExtensions.add(
                    ActivatedGalleryExtension(
                        type = keyExtension,
                        key = encodedKey,
                        expiresAt = keyExpiresAt,
                    )
                )

                if (alreadyActivatedExtension != null) {
                    alreadyActivatedExtensions.remove(alreadyActivatedExtension)
                }
            }
        }

        if (newActivatedExtensions.isNotEmpty()) {
            stateSubject.onNext(
                currentState.copy(
                    primarySubject = keySubject,
                    activatedExtensions = alreadyActivatedExtensions + newActivatedExtensions,
                )
            )
        }

        return newActivatedExtensions
    }
}
