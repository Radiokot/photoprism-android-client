package ua.com.radiokot.photoprism.features.ext.di

import org.koin.core.qualifier._q
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module
import ua.com.radiokot.photoprism.base.data.storage.ObjectPersistence
import ua.com.radiokot.photoprism.di.APP_NO_BACKUP_PREFERENCES
import ua.com.radiokot.photoprism.features.ext.data.model.GalleryExtensionsState
import ua.com.radiokot.photoprism.features.ext.data.storage.GalleryExtensionsStatePersistenceOnPrefs
import ua.com.radiokot.photoprism.features.ext.data.storage.GalleryExtensionsStateRepository

val galleryExtensionsFeatureModule = module {
    single<ObjectPersistence<GalleryExtensionsState>>(_q<GalleryExtensionsState>()) {
        GalleryExtensionsStatePersistenceOnPrefs(
            key = "ext",
            preferences = get(named(APP_NO_BACKUP_PREFERENCES)),
            jsonObjectMapper = get(),
        )
    }

    single {
        GalleryExtensionsStateRepository(
            statePersistence = get(_q<GalleryExtensionsState>())
        )
    } bind GalleryExtensionsStateRepository::class
}
