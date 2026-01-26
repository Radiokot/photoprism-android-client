package ua.com.radiokot.photoprism.features.map.data.storage

import io.reactivex.rxjava3.core.Single
import ua.com.radiokot.photoprism.api.geo.service.PhotoPrismGeoService
import ua.com.radiokot.photoprism.base.data.storage.SimpleSingleItemRepository
import ua.com.radiokot.photoprism.extension.toSingle

/**
 * A repository that holds GeoJSON used to show all the library photos on the map.
 */
class MapGeoJsonRepository(
    private val photoPrismGeoService: PhotoPrismGeoService,
) : SimpleSingleItemRepository<String>() {
    override fun getItem(): Single<String> = {
        photoPrismGeoService
            .getGeoJson(
                count = 500000,
                offset = 0,
                public = true,
            )
    }
        .toSingle()
}
