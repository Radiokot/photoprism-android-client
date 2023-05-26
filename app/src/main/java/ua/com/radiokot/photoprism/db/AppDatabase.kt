package ua.com.radiokot.photoprism.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import ua.com.radiokot.photoprism.features.gallery.data.model.SearchBookmarksDbEntity
import ua.com.radiokot.photoprism.features.gallery.data.storage.SearchBookmarksDbDao

@Database(
    version = 4,
    entities = [
        SearchBookmarksDbEntity::class,
    ],
    exportSchema = true,
)
@TypeConverters(
    value = [
        AppDatabaseConverters::class,
    ]
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun bookmarks(): SearchBookmarksDbDao
}