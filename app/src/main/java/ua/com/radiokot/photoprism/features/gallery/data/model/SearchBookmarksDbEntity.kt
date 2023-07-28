package ua.com.radiokot.photoprism.features.gallery.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    "bookmarks",
    indices = [
        Index(value = ["position"])
    ]
)
data class SearchBookmarksDbEntity(
    @PrimaryKey
    val id: Long,
    @ColumnInfo("position", typeAffinity = ColumnInfo.REAL)
    val position: Double,
    @ColumnInfo("name")
    val name: String,
    @ColumnInfo("user_query")
    val userQuery: String?,
    @ColumnInfo("media_types")
    val mediaTypes: List<String>?,
    @ColumnInfo("include_private")
    val includePrivate: Boolean,
    @ColumnInfo("album_uid")
    val albumUid: String?,
    @ColumnInfo("person_ids", defaultValue = "[]")
    val personIds: List<String>,
)
