package ua.com.radiokot.photoprism.features.gallery.view.model

import ua.com.radiokot.photoprism.features.gallery.data.model.GalleryMedia

data class AppliedGallerySearch(
    val mediaTypes: Set<GalleryMedia.TypeName>,
    val userQuery: String?,
)