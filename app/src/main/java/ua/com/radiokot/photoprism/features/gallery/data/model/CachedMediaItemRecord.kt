package ua.com.radiokot.photoprism.features.gallery.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cached_media_items")
data class CachedMediaItemRecord(
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0,
    @ColumnInfo(index = true) val hash: String,
    val cachedAt: Long,
    var lastHitAt: Long,
    val localPath: String,
    val size: Long,
)
