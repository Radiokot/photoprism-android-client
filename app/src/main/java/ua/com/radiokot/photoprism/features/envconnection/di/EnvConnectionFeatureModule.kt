package ua.com.radiokot.photoprism.features.envconnection.di

import android.content.Context
import android.content.SharedPreferences
import org.koin.android.ext.koin.androidApplication
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.qualifier._q
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module
import ua.com.radiokot.photoprism.base.data.storage.ObjectPersistence
import ua.com.radiokot.photoprism.di.EnvPhotoPrismClientConfigServiceParams
import ua.com.radiokot.photoprism.di.IMAGE_CACHE_DIRECTORY
import ua.com.radiokot.photoprism.di.VIDEO_CACHE_DIRECTORY
import ua.com.radiokot.photoprism.di.envModule
import ua.com.radiokot.photoprism.env.data.model.EnvAuth
import ua.com.radiokot.photoprism.env.data.model.EnvSession
import ua.com.radiokot.photoprism.env.data.storage.EnvSessionHolder
import ua.com.radiokot.photoprism.env.data.storage.KoinScopeEnvSessionHolder
import ua.com.radiokot.photoprism.features.envconnection.data.storage.EncryptedSharedPreferencesRemovalMigration
import ua.com.radiokot.photoprism.features.envconnection.data.storage.EnvAuthPersistenceOnPrefs
import ua.com.radiokot.photoprism.features.envconnection.data.storage.EnvSessionPersistenceOnPrefs
import ua.com.radiokot.photoprism.features.envconnection.logic.ConnectToEnvUseCase
import ua.com.radiokot.photoprism.features.envconnection.logic.DisconnectFromEnvUseCase
import ua.com.radiokot.photoprism.features.envconnection.view.model.EnvConnectionViewModel

private const val ENV_PREFERENCES = "env"

val envConnectionFeatureModule = module {
    includes(envModule)

    single(named(ENV_PREFERENCES)) {
        androidApplication().getSharedPreferences(
            "env",
            Context.MODE_PRIVATE,
        ).also { envPreferences ->
            val encryptedSharedPreferencesRemovalMigration =
                EncryptedSharedPreferencesRemovalMigration(
                    context = get(),
                )
            if (encryptedSharedPreferencesRemovalMigration.isApplicable) {
                encryptedSharedPreferencesRemovalMigration.apply(envPreferences)
            }
        }
    } bind SharedPreferences::class

    single<ObjectPersistence<EnvSession>>(_q<EnvSession>()) {
        EnvSessionPersistenceOnPrefs(
            key = "session",
            preferences = get(named(ENV_PREFERENCES)),
            jsonObjectMapper = get(),
        )
    }

    single<ObjectPersistence<EnvAuth>>(_q<EnvAuth>()) {
        EnvAuthPersistenceOnPrefs(
            key = "auth",
            preferences = get(named(ENV_PREFERENCES)),
            jsonObjectMapper = get(),
        )
    }

    single {
        KoinScopeEnvSessionHolder(
            koin = getKoin(),
        )
    }.bind(EnvSessionHolder::class)

    factory {
        ConnectToEnvUseCase(
            sessionCreatorFactory = get(),
            configServiceFactory = { envConnectionParams, sessionId ->
                get(_q<EnvPhotoPrismClientConfigServiceParams>()) {
                    EnvPhotoPrismClientConfigServiceParams(
                        envConnectionParams = envConnectionParams,
                        sessionId = sessionId,
                    )
                }
            },
            envSessionHolder = get(),
            envSessionPersistence = getOrNull(_q<EnvSession>()),
            envAuthPersistence = get(_q<EnvAuth>()),
            application = androidApplication(),
        )
    } bind ConnectToEnvUseCase::class

    scope<EnvSession> {
        factory {
            DisconnectFromEnvUseCase(
                envSessionHolder = get(),
                envSessionPersistence = getOrNull(_q<EnvSession>()),
                envAuthPersistence = getOrNull(_q<EnvAuth>()),
                cacheDirectories = listOf(
                    get(named(IMAGE_CACHE_DIRECTORY)),
                    get(named(VIDEO_CACHE_DIRECTORY)),
                ),
                cookieManager = getOrNull(),
                memoriesRepository = getOrNull(),
                application = androidApplication(),
            )
        } bind DisconnectFromEnvUseCase::class
    }

    viewModel {
        EnvConnectionViewModel(
            connectToEnvUseCase = get(),
            demoRootUrl = getProperty("demoLibraryRootUrl"),
        )
    }
}
