package ua.com.radiokot.photoprism.di

import androidx.room.Room
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
                .addMigrations()
                .build()
        } bind AppDatabase::class
    }
)