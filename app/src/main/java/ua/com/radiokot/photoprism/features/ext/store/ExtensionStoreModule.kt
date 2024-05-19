package ua.com.radiokot.photoprism.features.ext.store

import com.fasterxml.jackson.databind.ObjectMapper
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.module.dsl.singleOf
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module
import ua.com.radiokot.photoprism.di.localeModule
import ua.com.radiokot.photoprism.di.retrofitApiModule
import ua.com.radiokot.photoprism.features.ext.key.activation.di.keyActivationFeatureModule
import ua.com.radiokot.photoprism.features.ext.store.api.FeatureStoreService
import ua.com.radiokot.photoprism.features.ext.store.api.model.FeaturesOnSaleResponse
import ua.com.radiokot.photoprism.features.ext.store.data.storage.GalleryExtensionsOnSaleRepository
import ua.com.radiokot.photoprism.features.ext.store.view.model.GalleryExtensionStoreViewModel
import java.text.NumberFormat

const val CURRENCY_NUMBER_FORMAT = "currency-number-format"

val extensionStoreModule = module {
    includes(
        retrofitApiModule,
        localeModule,
        keyActivationFeatureModule,
    )

//    single {
//        get<Retrofit>(_q<EnvRetrofitParams>()) {
//            EnvRetrofitParams(
//                apiUrl = "http://10.0.0.125:8041".toHttpUrl(),
//                httpClient = get(),
//            )
//        }.create(FeatureStoreService::class.java)
//    } bind FeatureStoreService::class

    single {
        object : FeatureStoreService {
            override fun getFeaturesOnSale(): FeaturesOnSaleResponse {
                Thread.sleep(1000)

                return get<ObjectMapper>().readValue(
                    """
                        {
                            "data": [
                                {
                                    "type": "features",
                                    "id": "0",
                                    "attributes": {
                                        "name": "The feature",
                                        "price": "2"
                                    }
                                },
                                {
                                    "type": "features",
                                    "id": "1",
                                    "attributes": {
                                        "name": "Yet another extension",
                                        "price": "25"
                                    }
                                }
                            ]
                        }
                    """.trimIndent(),
                    FeaturesOnSaleResponse::class.java
                )
            }
        }
    } bind FeatureStoreService::class

    singleOf(::GalleryExtensionsOnSaleRepository)

    factory(named(CURRENCY_NUMBER_FORMAT)) {
        NumberFormat.getCurrencyInstance(get())
    } bind java.text.NumberFormat::class

    viewModel {
        GalleryExtensionStoreViewModel(
            extensionsOnSaleRepository = get(),
            galleryExtensionsStateRepository = get(),
            onlinePurchaseBaseUrl = "http://10.0.0.125:8041/buy".toHttpUrl(),
            hardwareIdentifier = get(),
        )
    }
}
