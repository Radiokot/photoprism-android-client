package ua.com.radiokot.photoprism.features.envconnection.di

import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences.PrefKeyEncryptionScheme
import androidx.security.crypto.EncryptedSharedPreferences.PrefValueEncryptionScheme
import androidx.security.crypto.MasterKey
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.module.Module
import org.koin.core.parameter.parametersOf
import org.koin.core.qualifier._q
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module
import ua.com.radiokot.photoprism.base.data.storage.ObjectPersistence
import ua.com.radiokot.photoprism.base.data.storage.ObjectPersistenceOnPrefs
import ua.com.radiokot.photoprism.di.envModules
import ua.com.radiokot.photoprism.env.data.model.EnvAuth
import ua.com.radiokot.photoprism.env.data.model.EnvSession
import ua.com.radiokot.photoprism.env.data.storage.EnvAuthPersistenceOnPrefs
import ua.com.radiokot.photoprism.env.data.storage.EnvSessionHolder
import ua.com.radiokot.photoprism.env.data.storage.KoinScopeEnvSessionHolder
import ua.com.radiokot.photoprism.features.envconnection.data.model.EnvConnection
import ua.com.radiokot.photoprism.features.envconnection.logic.ConnectToEnvUseCase
import ua.com.radiokot.photoprism.features.envconnection.view.model.EnvConnectionViewModel

private const val AUTH_PREFERENCES = "auth"

val envConnectionFeatureModules: List<Module> = listOf(
    module {
        includes(envModules)

        single(named(AUTH_PREFERENCES)) {
            EncryptedSharedPreferences.create(
                get(),
                "auth",
                MasterKey.Builder(get(), "auth")
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build(),
                PrefKeyEncryptionScheme.AES256_SIV,
                PrefValueEncryptionScheme.AES256_GCM,
            )
        } bind SharedPreferences::class

        single<ObjectPersistence<EnvSession>>(_q<EnvSession>()) {
            ObjectPersistenceOnPrefs.forType(
                key = "session",
                preferences = get(named(AUTH_PREFERENCES)),
                jsonObjectMapper = get(),
            )
        }

        single<ObjectPersistence<EnvAuth>>(_q<EnvAuth>()) {
            EnvAuthPersistenceOnPrefs(
                key = "auth",
                preferences = get(named(AUTH_PREFERENCES)),
                jsonObjectMapper = get(),
            )
        }

        single {
            KoinScopeEnvSessionHolder(
                koin = getKoin(),
            )
        }.bind(EnvSessionHolder::class)

        factory { (connection: EnvConnection) ->
            ConnectToEnvUseCase(
                connection = connection,
                sessionCreator = get { parametersOf(connection.apiUrl) },
                configServiceFactory = { apiUrl, sessionId ->
                    get { parametersOf(apiUrl, sessionId) }
                },
                envSessionHolder = get(),
                envSessionPersistence = getOrNull(_q<EnvSession>()),
                envAuthPersistence = get(_q<EnvAuth>()),
            )
        }

        viewModel {
            EnvConnectionViewModel(
                connectUseCaseProvider = { connection ->
                    get { parametersOf(connection) }
                },
            )
        }
    },
)