package ua.com.radiokot.photoprism.features.ext.store

import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module
import ua.com.radiokot.photoprism.di.APP_NO_BACKUP_PREFERENCES
import ua.com.radiokot.photoprism.di.localeModule
import ua.com.radiokot.photoprism.di.retrofitApiModule
import ua.com.radiokot.photoprism.extension.checkNotNull
import ua.com.radiokot.photoprism.features.ext.data.model.GalleryExtension
import ua.com.radiokot.photoprism.features.ext.data.storage.GalleryExtensionsStateRepository
import ua.com.radiokot.photoprism.features.ext.key.activation.keyActivationFeatureModule
import ua.com.radiokot.photoprism.features.ext.key.logic.HardwareIdentifier
import ua.com.radiokot.photoprism.features.ext.key.renewal.keyRenewalFeatureModule
import ua.com.radiokot.photoprism.features.ext.store.data.storage.GalleryExtensionStorePreferences
import ua.com.radiokot.photoprism.features.ext.store.data.storage.GalleryExtensionStorePreferencesOnPrefs
import ua.com.radiokot.photoprism.features.ext.store.data.storage.GalleryExtensionsOnSaleRepository
import ua.com.radiokot.photoprism.features.ext.store.view.model.GalleryExtensionStoreViewModel
import java.text.NumberFormat

const val CURRENCY_NUMBER_FORMAT = "currency-number-format"

val galleryExtensionStoreModule = module {
    includes(
        retrofitApiModule,
        localeModule,
        keyActivationFeatureModule,
        keyRenewalFeatureModule,
    )

    singleOf(::GalleryExtensionsOnSaleRepository)

    factory(named(CURRENCY_NUMBER_FORMAT)) {
        NumberFormat.getCurrencyInstance(get())
    } bind NumberFormat::class

    single {
        GalleryExtensionStorePreferencesOnPrefs(
            preferences = get(named(APP_NO_BACKUP_PREFERENCES)),
            keyPrefix = "extension_store",
        )
    } bind GalleryExtensionStorePreferences::class

    viewModel {
        val galleryExtensionsStateRepository: GalleryExtensionsStateRepository = get()
        val hardwareIdentifier: HardwareIdentifier = get()
        val featureStoreRootUrl: HttpUrl = getProperty<String>("offlineLicenseKeySvcRootUrl")
            .checkNotNull { "Missing feature store root URL" }
            .toHttpUrl()

        GalleryExtensionStoreViewModel(
            extensionsOnSaleRepository = get(),
            galleryExtensionsStateRepository = galleryExtensionsStateRepository,
            storePreferences = get(),
            onlinePurchaseUrlFactory = { extension: GalleryExtension ->
                featureStoreRootUrl
                    .newBuilder()
                    .addPathSegment("buy")
                    .addQueryParameter(
                        "f",
                        extension.ordinal.toString()
                    )
                    .addQueryParameter(
                        "hw",
                        hardwareIdentifier.getHardwareIdentifier()
                    )
                    .addQueryParameter(
                        "email",
                        galleryExtensionsStateRepository.currentState.primarySubject
                    )
                    .build()
            },
        )
    }
}
