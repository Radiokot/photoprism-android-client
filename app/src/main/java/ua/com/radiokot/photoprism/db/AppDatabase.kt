package ua.com.radiokot.photoprism.db

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import ua.com.radiokot.photoprism.features.ext.memories.data.model.MemoryDbEntity
import ua.com.radiokot.photoprism.features.ext.memories.data.storage.MemoriesDbDao
import ua.com.radiokot.photoprism.features.gallery.data.model.SearchBookmarksDbEntity
import ua.com.radiokot.photoprism.features.gallery.data.storage.SearchBookmarksDbDao
import ua.com.radiokot.photoprism.features.gallery.data.model.CachedMediaItemRecord
import ua.com.radiokot.photoprism.features.gallery.data.storage.CachedMediaItemDao

@Database(
    version = 9,
    entities = [
        SearchBookmarksDbEntity::class,
        MemoryDbEntity::class,
        CachedMediaItemRecord::class,
    ],
    autoMigrations = [
        AutoMigration(from = 4, to = 5),
        AutoMigration(from = 5, to = 6),
        AutoMigration(from = 6, to = 7),
        AutoMigration(from = 7, to = 8),
        AutoMigration(from = 8, to = 9),
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
    abstract fun memories(): MemoriesDbDao
    abstract fun cachedMediaItems(): CachedMediaItemDao
}
