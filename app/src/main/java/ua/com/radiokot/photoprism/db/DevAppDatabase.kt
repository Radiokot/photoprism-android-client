package ua.com.radiokot.photoprism.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import ua.com.radiokot.photoprism.features.ext.memories.data.model.MemoryDbEntity
import ua.com.radiokot.photoprism.features.ext.memories.data.storage.MemoriesDbDao


@Database(
    version = 1,
    entities = [
        MemoryDbEntity::class,
    ],
    exportSchema = false,
)
@TypeConverters(
    value = [
        AppDatabaseConverters::class,
    ]
)
/**
 * A database for features in development.
 * It is not available in release builds.
 */
abstract class DevAppDatabase : RoomDatabase() {
    abstract fun memories(): MemoriesDbDao
}
