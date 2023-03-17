package ua.com.radiokot.photoprism.features.env.di

import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences.PrefKeyEncryptionScheme
import androidx.security.crypto.EncryptedSharedPreferences.PrefValueEncryptionScheme
import org.koin.core.module.Module
import org.koin.core.qualifier.qualifier
import org.koin.dsl.module
import ua.com.radiokot.photoprism.base.data.storage.ObjectPersistence
import ua.com.radiokot.photoprism.base.data.storage.ObjectPersistenceOnPrefs
import ua.com.radiokot.photoprism.di.httpModules
import ua.com.radiokot.photoprism.features.env.data.model.PhotoPrismSession

val envFeatureModules: List<Module> = listOf(
    module {
        includes(httpModules)

        single<ObjectPersistence<PhotoPrismSession>>(qualifier<PhotoPrismSession>()) {
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
    },
)