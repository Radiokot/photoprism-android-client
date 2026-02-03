package ua.com.radiokot.photoprism.features.map

import org.koin.android.ext.koin.androidApplication
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module
import ua.com.radiokot.photoprism.di.APP_NO_BACKUP_PREFERENCES
import ua.com.radiokot.photoprism.di.MAP_CACHE_DIRECTORY
import ua.com.radiokot.photoprism.di.retrofitApiModule
import ua.com.radiokot.photoprism.env.data.model.EnvSession
import ua.com.radiokot.photoprism.extension.checkNotNull
import ua.com.radiokot.photoprism.features.map.data.storage.MapGeoJsonRepository
import ua.com.radiokot.photoprism.features.map.data.storage.MapPreferences
import ua.com.radiokot.photoprism.features.map.data.storage.MapPreferencesOnPrefs
import ua.com.radiokot.photoprism.features.map.view.MapViewModel

val mapFeatureModule = module {
    includes(
        retrofitApiModule,
    )

    single {
        MapPreferencesOnPrefs(
            preferences = get(named(APP_NO_BACKUP_PREFERENCES)),
            keyPrefix = "map",
        )
    } bind MapPreferences::class

    scope<EnvSession> {
        scoped {
            MapGeoJsonRepository(
                photoPrismGeoService = get(),
            )
        }

        viewModel {
            MapViewModel(
                geoJsonRepository = get(),
                mapCacheDirectory = get(named(MAP_CACHE_DIRECTORY)),
                mapPreferences = get(),
                defaultMapStyleUrl =
                    getProperty<String>("defaultMapStyleUrl")
                        .checkNotNull { "Missing default map style URL" },
                application = androidApplication(),
            )
        }
    }
}
