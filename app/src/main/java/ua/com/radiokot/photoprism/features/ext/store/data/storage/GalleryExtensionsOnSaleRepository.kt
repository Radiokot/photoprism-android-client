package ua.com.radiokot.photoprism.features.ext.store.data.storage

import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import ua.com.radiokot.photoprism.base.data.storage.SimpleCollectionRepository
import ua.com.radiokot.photoprism.extension.mapSuccessful
import ua.com.radiokot.photoprism.extension.toSingle
import ua.com.radiokot.photoprism.features.ext.api.OfflineLicenseKeyService
import ua.com.radiokot.photoprism.features.ext.store.data.model.GalleryExtensionOnSale

class GalleryExtensionsOnSaleRepository(
    private val offlineLicenseKeyService: OfflineLicenseKeyService,
) : SimpleCollectionRepository<GalleryExtensionOnSale>() {
    override fun getCollection(): Single<List<GalleryExtensionOnSale>> = {
        offlineLicenseKeyService.getFeaturesOnSale()
            .data
            .mapSuccessful(::GalleryExtensionOnSale)
    }.toSingle().subscribeOn(Schedulers.io())
}
