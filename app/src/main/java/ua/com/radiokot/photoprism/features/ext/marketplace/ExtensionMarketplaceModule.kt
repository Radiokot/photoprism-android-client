package ua.com.radiokot.photoprism.features.ext.marketplace

import okhttp3.HttpUrl.Companion.toHttpUrl
import org.koin.androidx.viewmodel.dsl.viewModelOf
import org.koin.core.module.dsl.singleOf
import org.koin.core.qualifier._q
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module
import retrofit2.Retrofit
import ua.com.radiokot.photoprism.di.EnvRetrofitParams
import ua.com.radiokot.photoprism.di.localeModule
import ua.com.radiokot.photoprism.di.retrofitApiModule
import ua.com.radiokot.photoprism.features.ext.marketplace.api.FeatureMarketplaceService
import ua.com.radiokot.photoprism.features.ext.marketplace.data.storage.GalleryExtensionsOnSaleRepository
import ua.com.radiokot.photoprism.features.ext.marketplace.view.model.GalleryExtensionMarketplaceViewModel
import java.text.NumberFormat

const val CURRENCY_NUMBER_FORMAT = "currency-number-format"

val extensionMarketplaceModule = module {
    includes(
        retrofitApiModule,
        localeModule,
    )

    single {
        get<Retrofit>(_q<EnvRetrofitParams>()) {
            EnvRetrofitParams(
                apiUrl = "http://10.0.0.125:8041".toHttpUrl(),
                httpClient = get(),
            )
        }.create(FeatureMarketplaceService::class.java)
    } bind FeatureMarketplaceService::class

    singleOf(::GalleryExtensionsOnSaleRepository)

    factory(named(CURRENCY_NUMBER_FORMAT)) {
        NumberFormat.getCurrencyInstance(get())
    } bind java.text.NumberFormat::class

    viewModelOf(::GalleryExtensionMarketplaceViewModel)
}
