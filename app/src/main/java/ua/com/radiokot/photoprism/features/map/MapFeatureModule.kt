package ua.com.radiokot.photoprism.features.map

import org.koin.dsl.module
import ua.com.radiokot.photoprism.env.data.model.EnvSession
import ua.com.radiokot.photoprism.features.map.data.storage.MapGeoJsonRepository

val mapFeatureModule = module {
    scope<EnvSession> {
        scoped {
            MapGeoJsonRepository(
                photoPrismGeoService = get(),
            )
        }
    }
}
