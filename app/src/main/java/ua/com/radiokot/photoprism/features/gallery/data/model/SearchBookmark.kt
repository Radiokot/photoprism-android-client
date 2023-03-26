package ua.com.radiokot.photoprism.features.gallery.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class SearchBookmark(
    val id: Long,
    val name: String,
    val searchConfig: SearchConfig,
) : Parcelable {
    constructor(dbEntity: SearchBookmarksDbEntity) : this(
        id = dbEntity.id,
        name = dbEntity.name,
        searchConfig = SearchConfig(
            mediaTypes = dbEntity.mediaTypes
                .map { GalleryMedia.TypeName.valueOf(it) }
                .toSet(),
            userQuery = dbEntity.userQuery,
        )
    )

    fun toDbEntity() = SearchBookmarksDbEntity(
        id = id,
        name = name,
        userQuery = searchConfig.userQuery,
        mediaTypes = searchConfig.mediaTypes.map(GalleryMedia.TypeName::toString)
    )

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