package ua.com.radiokot.photoprism.features.welcome.di

import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module
import ua.com.radiokot.photoprism.di.APP_NO_BACKUP_PREFERENCES
import ua.com.radiokot.photoprism.di.ioModules
import ua.com.radiokot.photoprism.features.welcome.data.storage.WelcomeScreenPreferences
import ua.com.radiokot.photoprism.features.welcome.data.storage.WelcomeScreenPreferencesOnPrefs

val welcomeScreenFeatureModules: List<Module> = listOf(
    module {
        includes(ioModules)

        single {
            WelcomeScreenPreferencesOnPrefs(
                preferences = get(named(APP_NO_BACKUP_PREFERENCES)),
                keyPrefix = "welcome"
            )
//            object : WelcomeScreenPreferences {
//                override var isWelcomeScreenEverShown: Boolean
//                    get() = false
//                    set(value) {}
//            }
        } bind WelcomeScreenPreferences::class
    }
)
