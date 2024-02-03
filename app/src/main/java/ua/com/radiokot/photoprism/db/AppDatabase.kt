package ua.com.radiokot.photoprism.db

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import ua.com.radiokot.photoprism.features.gallery.data.model.SearchBookmarksDbEntity
import ua.com.radiokot.photoprism.features.gallery.data.storage.SearchBookmarksDbDao
import ua.com.radiokot.photoprism.features.memories.data.model.MemoryDbEntity
import ua.com.radiokot.photoprism.features.memories.data.storage.MemoriesDbDao

@Database(
    version = 7,
    entities = [
        SearchBookmarksDbEntity::class,
        MemoryDbEntity::class,
    ],
    autoMigrations = [
        AutoMigration(from = 4, to = 5),
        AutoMigration(from = 5, to = 6),
        AutoMigration(from = 6, to = 7),
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
}
