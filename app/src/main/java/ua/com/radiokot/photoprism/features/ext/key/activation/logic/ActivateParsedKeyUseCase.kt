package ua.com.radiokot.photoprism.features.ext.key.activation.logic

import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.kotlin.toCompletable
import ua.com.radiokot.photoprism.extension.toSingle
import ua.com.radiokot.photoprism.features.ext.data.model.ActivatedGalleryExtension
import ua.com.radiokot.photoprism.features.ext.data.model.GalleryExtension
import ua.com.radiokot.photoprism.features.ext.data.storage.GalleryExtensionsStateRepository
import ua.com.radiokot.photoprism.features.ext.key.activation.data.model.ParsedKey
import ua.com.radiokot.photoprism.features.ext.memories.logic.ScheduleDailyMemoriesUpdatesUseCase
import ua.com.radiokot.photoprism.features.widgets.photoframe.logic.UpdatePhotoFrameWidgetManifestComponentsUseCase

class ActivateParsedKeyUseCase(
    private val galleryExtensionsStateRepository: GalleryExtensionsStateRepository,
    private val scheduleDailyMemoriesUpdatesUseCase: ScheduleDailyMemoriesUpdatesUseCase,
    private val updatePhotoFrameWidgetManifestComponentsUseCase: UpdatePhotoFrameWidgetManifestComponentsUseCase,
) {
    operator fun invoke(
        parsedKey: ParsedKey,
    ): Single<Collection<ActivatedGalleryExtension>> {
        lateinit var activatedExtensions: Collection<ActivatedGalleryExtension>

        return activateKeyExtensions(parsedKey)
            .doOnSuccess { activatedExtensions = it }
            .flatMapCompletable(::handleNewActivatedExtensions)
            .toSingle { activatedExtensions }
    }

    private fun activateKeyExtensions(
        parsedKey: ParsedKey,
    ): Single<Collection<ActivatedGalleryExtension>> = {
        galleryExtensionsStateRepository.activateKeyExtensions(
            keySubject = parsedKey.subject,
            keyExtensions = parsedKey.extensions,
            keyExpiresAt = parsedKey.expiresAt,
            encodedKey = parsedKey.encoded,
        )
    }.toSingle()

    private fun handleNewActivatedExtensions(
        newActivatedExtensions: Collection<ActivatedGalleryExtension>,
    ): Completable = {
        val newActivatedExtensionsSet = newActivatedExtensions
            .mapTo(mutableSetOf(), ActivatedGalleryExtension::type)

        if (GalleryExtension.MEMORIES in newActivatedExtensionsSet) {
            scheduleDailyMemoriesUpdatesUseCase.invoke()
        }

        if (GalleryExtension.PHOTO_FRAME_WIDGET in newActivatedExtensionsSet) {
            updatePhotoFrameWidgetManifestComponentsUseCase.invoke()
        }
    }.toCompletable()
}
