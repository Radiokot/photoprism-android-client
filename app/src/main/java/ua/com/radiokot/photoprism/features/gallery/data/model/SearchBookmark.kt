package ua.com.radiokot.photoprism.features.gallery.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class SearchBookmark(
    val id: Long,
    val position: Double,
    val name: String,
    val searchConfig: SearchConfig,
) : Parcelable, Comparable<SearchBookmark> {
    constructor(dbEntity: SearchBookmarksDbEntity) : this(
        id = dbEntity.id,
        name = dbEntity.name,
        position = dbEntity.position,
        searchConfig = SearchConfig(
            mediaTypes = dbEntity.mediaTypes
                .map { GalleryMedia.TypeName.valueOf(it) }
                .toSet(),
            before = null,
            userQuery = dbEntity.userQuery ?: "",
            includePrivate = dbEntity.includePrivate,
        )
    )

    fun toDbEntity() = SearchBookmarksDbEntity(
        id = id,
        name = name,
        position = position,
        userQuery = searchConfig.userQuery,
        mediaTypes = searchConfig.mediaTypes.map(GalleryMedia.TypeName::toString),
        includePrivate = searchConfig.includePrivate,
    )

    override fun compareTo(other: SearchBookmark): Int = position.compareTo(other.position)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SearchBookmark

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}