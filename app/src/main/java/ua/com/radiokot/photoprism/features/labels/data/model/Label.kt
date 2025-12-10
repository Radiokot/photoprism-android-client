package ua.com.radiokot.photoprism.features.labels.data.model

import ua.com.radiokot.photoprism.api.labels.model.PhotoPrismLabel

class Label(
    val uid: String,
    val name: String,
    val isFavorite: Boolean,
    val slug: String,
    val itemCount: Int,
    val thumbnailHash: String,
) {
    constructor(
        source: PhotoPrismLabel,
    ): this(
        uid = source.uid,
        name = source.name,
        isFavorite = source.favorite,
        slug = source.customSlug,
        itemCount = source.photoCount,
        thumbnailHash = source.thumb,
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Label) return false

        if (uid != other.uid) return false

        return true
    }

    override fun hashCode(): Int {
        return uid.hashCode()
    }

    override fun toString(): String {
        return "Label(name='$name', uid='$uid')"
    }
}
