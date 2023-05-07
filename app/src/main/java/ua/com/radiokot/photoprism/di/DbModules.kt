package ua.com.radiokot.photoprism.di

import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import org.koin.dsl.bind
import org.koin.dsl.module
import ua.com.radiokot.photoprism.db.AppDatabase

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
                    object : Migration(1, 2) {
                        override fun migrate(database: SupportSQLiteDatabase) = with(database) {
                            execSQL("ALTER TABLE `bookmarks` ADD COLUMN `include_private` INTEGER NOT NULL DEFAULT 0")
                        }
                    },
                    object : Migration(2, 3) {
                        override fun migrate(database: SupportSQLiteDatabase)= with(database) {
                            execSQL("ALTER TABLE `bookmarks` ADD COLUMN `album_uid` TEXT")
                        }
                    }
                )
                .build()
        } bind AppDatabase::class
    }
)