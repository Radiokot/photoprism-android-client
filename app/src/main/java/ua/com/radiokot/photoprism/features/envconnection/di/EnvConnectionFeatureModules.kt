package ua.com.radiokot.photoprism.features.envconnection.di

import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences.PrefKeyEncryptionScheme
import androidx.security.crypto.EncryptedSharedPreferences.PrefValueEncryptionScheme
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.module.Module
import org.koin.core.parameter.parametersOf
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module
import ua.com.radiokot.photoprism.base.data.storage.ObjectPersistence
import ua.com.radiokot.photoprism.base.data.storage.ObjectPersistenceOnPrefs
import ua.com.radiokot.photoprism.di.envModules
import ua.com.radiokot.photoprism.features.envconnection.data.model.EnvConnection
import ua.com.radiokot.photoprism.env.data.model.EnvSession
import ua.com.radiokot.photoprism.env.data.storage.EnvSessionHolder
import ua.com.radiokot.photoprism.env.data.storage.KoinScopeEnvSessionHolder
import ua.com.radiokot.photoprism.features.envconnection.logic.ConnectToEnvironmentUseCase
import ua.com.radiokot.photoprism.features.envconnection.view.model.EnvConnectionViewModel

val envConnectionFeatureModules: List<Module> = listOf(
    module {
        includes(envModules)

        single<ObjectPersistence<EnvSession>>(named<EnvSession>()) {
            ObjectPersistenceOnPrefs.forType(
                key = "session",
                preferences = EncryptedSharedPreferences.create(
                    "session",
                    "session",
                    get(),
                    PrefKeyEncryptionScheme.AES256_SIV,
                    PrefValueEncryptionScheme.AES256_GCM,
                ),
                jsonObjectMapper = get()
            )
        }

        single {
            KoinScopeEnvSessionHolder(
                koin = getKoin(),
            )
        }.bind(EnvSessionHolder::class)

        factory { (connection: EnvConnection) ->
            ConnectToEnvironmentUseCase(
                connection = connection,
                sessionCreator = get { parametersOf(connection.apiUrl) },
                configServiceFactory = { apiUrl, sessionId ->
                    get { parametersOf(apiUrl, sessionId) }
                },
                envSessionHolder = get(),
                envSessionPersistence = get(named<EnvSession>()),
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