package ua.com.radiokot.photoprism.di

import android.content.ContentValues
import androidx.room.OnConflictStrategy
import androidx.room.Room
import org.koin.dsl.bind
import org.koin.dsl.module
import ua.com.radiokot.photoprism.db.AppDatabase
import ua.com.radiokot.photoprism.db.roomMigration

val appDbModule = module {
    single {
        Room
            .databaseBuilder(
                context = get(),
                klass = AppDatabase::class.java,
                name = "database"
            )
            .addMigrations(
                roomMigration(from = 1, to = 2) {
                    execSQL("ALTER TABLE `bookmarks` ADD COLUMN `include_private` INTEGER NOT NULL DEFAULT 0")
                },
                roomMigration(from = 2, to = 3) {
                    execSQL("ALTER TABLE `bookmarks` ADD COLUMN `album_uid` TEXT")
                },
                roomMigration(from = 3, to = 4) {
                    execSQL("CREATE TABLE IF NOT EXISTS `_new_bookmarks` (`id` INTEGER NOT NULL, `position` REAL NOT NULL, `name` TEXT NOT NULL, `user_query` TEXT, `media_types` TEXT, `include_private` INTEGER NOT NULL, `album_uid` TEXT, PRIMARY KEY(`id`))")
                    execSQL("INSERT INTO `_new_bookmarks` (`id`,`position`,`name`,`user_query`,`media_types`,`include_private`,`album_uid`) SELECT `id`,`position`,`name`,`user_query`,`media_types`,`include_private`,`album_uid` FROM `bookmarks`")
                    execSQL("DROP TABLE `bookmarks`")
                    execSQL("ALTER TABLE `_new_bookmarks` RENAME TO `bookmarks`")
                    execSQL("CREATE INDEX IF NOT EXISTS `index_bookmarks_position` ON `bookmarks` (`position`)")

                    // Set [] media types to null.
                    val updateValues = ContentValues(1).apply {
                        putNull("media_types")
                    }
                    update(
                        "bookmarks",
                        OnConflictStrategy.ABORT,
                        updateValues,
                        "`media_types`=?",
                        arrayOf("[]")
                    )
                }
            )
            .build()
    } bind AppDatabase::class
}
