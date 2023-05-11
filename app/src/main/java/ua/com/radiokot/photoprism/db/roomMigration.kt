package ua.com.radiokot.photoprism.db

import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Clean lambda-based [androidx.room.migration.Migration] implementation.
 */
internal fun roomMigration(
    from: Int,
    to: Int,
    migration: SupportSQLiteDatabase.() -> Unit,
) = object : androidx.room.migration.Migration(from, to) {
    override fun migrate(database: SupportSQLiteDatabase) = database.run(migration)
}