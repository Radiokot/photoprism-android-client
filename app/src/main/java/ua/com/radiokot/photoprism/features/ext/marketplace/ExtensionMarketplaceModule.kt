package ua.com.radiokot.photoprism.features.ext.marketplace

import okhttp3.HttpUrl.Companion.toHttpUrl
import org.koin.core.module.dsl.singleOf
import org.koin.core.qualifier._q
import org.koin.dsl.bind
import org.koin.dsl.module
import retrofit2.Retrofit
import ua.com.radiokot.photoprism.di.EnvRetrofitParams
import ua.com.radiokot.photoprism.di.retrofitApiModule
import ua.com.radiokot.photoprism.features.ext.marketplace.api.FeatureMarketplaceService
import ua.com.radiokot.photoprism.features.ext.marketplace.data.storage.GalleryExtensionsOnSaleRepository

val extensionMarketplaceModule = module {
    includes(retrofitApiModule)

    single {
        get<Retrofit>(_q<EnvRetrofitParams>()) {
            EnvRetrofitParams(
                apiUrl = "http://10.0.0.125:8041".toHttpUrl(),
                httpClient = get(),
            )
        }.create(FeatureMarketplaceService::class.java)
    } bind FeatureMarketplaceService::class

    singleOf(::GalleryExtensionsOnSaleRepository)
}
