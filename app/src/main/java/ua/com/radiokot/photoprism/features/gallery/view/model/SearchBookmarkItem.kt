package ua.com.radiokot.photoprism.features.gallery.view.model

import ua.com.radiokot.photoprism.features.gallery.data.model.SearchBookmark

class SearchBookmarkItem(
    val name: String,
    val source: SearchBookmark?,
) {
    constructor(source: SearchBookmark) : this(
        name = source.name,
        source = source,
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SearchBookmarkItem

        if (name != other.name) return false
        if (source != other.source) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + (source?.hashCode() ?: 0)
        return result
    }

}