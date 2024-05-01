package ua.com.radiokot.photoprism.features.ext.marketplace.data.storage

import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import ua.com.radiokot.photoprism.base.data.storage.SimpleCollectionRepository
import ua.com.radiokot.photoprism.extension.mapSuccessful
import ua.com.radiokot.photoprism.extension.toSingle
import ua.com.radiokot.photoprism.features.ext.marketplace.api.FeatureMarketplaceService
import ua.com.radiokot.photoprism.features.ext.marketplace.data.model.GalleryExtensionOnSale

class GalleryExtensionsOnSaleRepository(
    private val featureMarketplaceService: FeatureMarketplaceService,
) : SimpleCollectionRepository<GalleryExtensionOnSale>() {
    override fun getCollection(): Single<List<GalleryExtensionOnSale>> = {
        featureMarketplaceService.getFeaturesOnSale()
            .data
            .mapSuccessful(::GalleryExtensionOnSale)
    }.toSingle().subscribeOn(Schedulers.io())
}
