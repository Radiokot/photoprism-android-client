package ua.com.radiokot.photoprism.features.ext.store

import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.module.dsl.singleOf
import org.koin.core.qualifier._q
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module
import retrofit2.Retrofit
import ua.com.radiokot.photoprism.di.EnvRetrofitParams
import ua.com.radiokot.photoprism.di.localeModule
import ua.com.radiokot.photoprism.di.retrofitApiModule
import ua.com.radiokot.photoprism.extension.checkNotNull
import ua.com.radiokot.photoprism.features.ext.data.model.GalleryExtension
import ua.com.radiokot.photoprism.features.ext.data.storage.GalleryExtensionsStateRepository
import ua.com.radiokot.photoprism.features.ext.key.activation.di.keyActivationFeatureModule
import ua.com.radiokot.photoprism.features.ext.key.logic.HardwareIdentifier
import ua.com.radiokot.photoprism.features.ext.store.api.FeatureStoreService
import ua.com.radiokot.photoprism.features.ext.store.data.storage.GalleryExtensionsOnSaleRepository
import ua.com.radiokot.photoprism.features.ext.store.view.model.GalleryExtensionStoreViewModel
import java.text.NumberFormat

const val CURRENCY_NUMBER_FORMAT = "currency-number-format"

val galleryExtensionStoreModule = module {
    includes(
        retrofitApiModule,
        localeModule,
        keyActivationFeatureModule,
    )

    single {
        get<Retrofit>(_q<EnvRetrofitParams>()) {
            EnvRetrofitParams(
                apiUrl = getProperty<String>("featureStoreRootUrl")
                    .checkNotNull { "Missing feature store root URL" }
                    .toHttpUrl(),
                httpClient = get(),
            )
        }.create(FeatureStoreService::class.java)
    } bind FeatureStoreService::class

    singleOf(::GalleryExtensionsOnSaleRepository)

    factory(named(CURRENCY_NUMBER_FORMAT)) {
        NumberFormat.getCurrencyInstance(get())
    } bind java.text.NumberFormat::class

    viewModel {
        val galleryExtensionsStateRepository: GalleryExtensionsStateRepository = get()
        val hardwareIdentifier: HardwareIdentifier = get()
        val featureStoreRootUrl: HttpUrl = getProperty<String>("featureStoreRootUrl")
            .checkNotNull { "Missing feature store root URL" }
            .toHttpUrl()

        GalleryExtensionStoreViewModel(
            extensionsOnSaleRepository = get(),
            galleryExtensionsStateRepository = galleryExtensionsStateRepository,
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
