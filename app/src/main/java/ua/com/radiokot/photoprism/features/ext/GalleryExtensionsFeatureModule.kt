package ua.com.radiokot.photoprism.features.ext

import okhttp3.HttpUrl.Companion.toHttpUrl
import org.koin.core.qualifier._q
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module
import retrofit2.Retrofit
import ua.com.radiokot.photoprism.base.data.storage.ObjectPersistence
import ua.com.radiokot.photoprism.di.APP_NO_BACKUP_PREFERENCES
import ua.com.radiokot.photoprism.di.EnvRetrofitParams
import ua.com.radiokot.photoprism.extension.checkNotNull
import ua.com.radiokot.photoprism.features.ext.api.OfflineLicenseKeyService
import ua.com.radiokot.photoprism.features.ext.data.model.GalleryExtensionsState
import ua.com.radiokot.photoprism.features.ext.data.storage.GalleryExtensionsStatePersistenceOnPrefs
import ua.com.radiokot.photoprism.features.ext.data.storage.GalleryExtensionsStateRepository

val galleryExtensionsFeatureModule = module {
    single {
        get<Retrofit>(_q<EnvRetrofitParams>()) {
            EnvRetrofitParams(
                apiUrl = getProperty<String>("offlineLicenseKeySvcRootUrl")
                    .checkNotNull { "Missing offline license key service root URL" }
                    .toHttpUrl(),
                httpClient = get(),
            )
        }.create(OfflineLicenseKeyService::class.java)
    } bind OfflineLicenseKeyService::class

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
