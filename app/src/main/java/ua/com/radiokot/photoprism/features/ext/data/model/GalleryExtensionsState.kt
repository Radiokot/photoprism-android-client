package ua.com.radiokot.photoprism.features.ext.data.model

data class GalleryExtensionsState(
    val activatedExtensions: List<ActivatedGalleryExtension> = emptyList(),
    val primarySubject: String? = null,
) {
    val activatedKeys: Set<String>
        get() = activatedExtensions.mapTo(mutableSetOf(), ActivatedGalleryExtension::key)
}
