package ua.com.radiokot.photoprism.di

import androidx.room.Room
import org.koin.dsl.bind
import org.koin.dsl.module
import ua.com.radiokot.photoprism.db.AppDatabase
import ua.com.radiokot.photoprism.db.roomMigration

val dbModules = listOf(
    module {
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
                )
                .build()
        } bind AppDatabase::class
    }
)