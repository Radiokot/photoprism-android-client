package ua.com.radiokot.photoprism.features.gallery.search.people.data.model

import ua.com.radiokot.photoprism.api.faces.model.PhotoPrismFace
import ua.com.radiokot.photoprism.api.subjects.model.PhotoPrismSubject
import ua.com.radiokot.photoprism.features.gallery.logic.MediaPreviewUrlFactory

/**
 * A person recognized by the library.
 * It is a single entity for two underlying sources: [PhotoPrismSubject] and [PhotoPrismFace]
 *
 * @see id
 * @see isFace
 */
class Person(
    val name: String?,
    /**
     * Subject UID or face ID.
     *
     * @see isFace
     * @see isFaceId
     * @see isSubjectUid
     */
    val id: String,
    val smallThumbnailUrl: String,
    val isFavorite: Boolean,
    /**
     * Whether this is a known person (subject) or just a face.
     */
    val isFace: Boolean,
) {
    val hasName: Boolean = name != null

    constructor(
        personSubject: PhotoPrismSubject,
        previewUrlFactory: MediaPreviewUrlFactory,
    ) : this(
        name = personSubject.name.takeIf(String::isNotEmpty),
        id = personSubject.uid,
        smallThumbnailUrl = previewUrlFactory.getSmallThumbnailUrl(personSubject.thumb),
        isFavorite = personSubject.favorite,
        isFace = false,
    ) {
        require(personSubject.type == "person") {
            "Expected person subject"
        }
    }

    constructor(
        face: PhotoPrismFace,
        previewUrlFactory: MediaPreviewUrlFactory,
    ) : this(
        name = face.name.takeIf(String::isNotEmpty),
        id = face.id,
        smallThumbnailUrl = previewUrlFactory.getSmallThumbnailUrl(face.thumb),
        isFavorite = false,
        isFace = true,
    ) {
        require(face.thumb.isNotEmpty()) {
            "The face must have a thumb, make sure it is requested with markers"
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Person) return false

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    override fun toString(): String {
        return "Person(name=$name, uid='$id', isFace=$isFace)"
    }

    companion object {
        private const val SUBJECT_UID_LENGTH = 16
        private const val FACE_ID_LENGTH = 32

        fun isSubjectUid(personId: String): Boolean =
            personId.length == SUBJECT_UID_LENGTH

        fun isFaceId(personId: String): Boolean =
            personId.length == FACE_ID_LENGTH
    }
}
