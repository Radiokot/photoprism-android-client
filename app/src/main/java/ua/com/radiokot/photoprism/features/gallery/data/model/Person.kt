package ua.com.radiokot.photoprism.features.gallery.data.model

import ua.com.radiokot.photoprism.api.faces.model.PhotoPrismFace
import ua.com.radiokot.photoprism.api.subjects.model.PhotoPrismSubject
import ua.com.radiokot.photoprism.features.gallery.logic.MediaPreviewUrlFactory

class Person(
    val name: String?,
    val uid: String,
    val smallThumbnailUrl: String,
    val isFavorite: Boolean,

    /**
     * Whether this is a known person or an unknown face.
     */
    val isUnknownFace: Boolean,
) {
    val hasName: Boolean = name != null

    constructor(
        personSubject: PhotoPrismSubject,
        previewUrlFactory: MediaPreviewUrlFactory,
    ) : this(
        name = personSubject.name.takeIf(String::isNotEmpty),
        uid = personSubject.uid,
        smallThumbnailUrl = previewUrlFactory.getSmallThumbnailUrl(personSubject.thumb),
        isFavorite = personSubject.favorite,
        isUnknownFace = false,
    ) {
        require(personSubject.type == "person") {
            "Expected person subject"
        }
    }

    constructor(
        unknownFace: PhotoPrismFace,
        previewUrlFactory: MediaPreviewUrlFactory,
    ) : this(
        name = null,
        uid = unknownFace.id,
        smallThumbnailUrl = previewUrlFactory.getSmallThumbnailUrl(unknownFace.thumb),
        isFavorite = false,
        isUnknownFace = true,
    ) {
        require(unknownFace.thumb.isNotEmpty()) {
            "The face must have a thumb, make sure it is requested with markers"
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Person) return false

        if (uid != other.uid) return false

        return true
    }

    override fun hashCode(): Int {
        return uid.hashCode()
    }

    override fun toString(): String {
        return "Person(name=$name, uid='$uid', isUnknownFace=$isUnknownFace)"
    }
}
