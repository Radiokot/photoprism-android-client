package ua.com.radiokot.photoprism.features.widgets.photoframe

import org.koin.core.module.dsl.scopedOf
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module
import ua.com.radiokot.photoprism.di.APP_NO_BACKUP_PREFERENCES
import ua.com.radiokot.photoprism.env.data.model.EnvSession
import ua.com.radiokot.photoprism.features.gallery.data.model.GalleryMedia
import ua.com.radiokot.photoprism.features.gallery.data.model.SearchConfig
import ua.com.radiokot.photoprism.features.widgets.photoframe.data.storage.PhotoFrameWidgetPreferencesOnPrefs
import ua.com.radiokot.photoprism.features.widgets.photoframe.data.storage.PhotoFrameWidgetsPreferences
import ua.com.radiokot.photoprism.features.widgets.photoframe.logic.ReloadPhotoFrameWidgetPhotoUseCase
import ua.com.radiokot.photoprism.features.widgets.photoframe.logic.UpdatePhotoFrameWidgetPhotoUseCase

private const val DEFAULT_SEARCH_CONFIG = "default-search-config"

val photoFrameWidgetFeatureModule = module {
    single(named(DEFAULT_SEARCH_CONFIG)) {
        SearchConfig.DEFAULT.copy(
            mediaTypes = setOf(
                GalleryMedia.TypeName.IMAGE,
                GalleryMedia.TypeName.RAW,
            ),
            // TODO: for aesthetic development only
            albumUid = "arp9ftzkv0jtxhh0",
        )
    }

    single {
        PhotoFrameWidgetPreferencesOnPrefs(
            keyPrefix = "photo_frame_widget",
            preferences = get(named(APP_NO_BACKUP_PREFERENCES)),
            jsonObjectMapper = get(),
            defaultSearchConfig = get(named(DEFAULT_SEARCH_CONFIG))
        )
    } bind PhotoFrameWidgetsPreferences::class

    scope<EnvSession> {
        scopedOf(::ReloadPhotoFrameWidgetPhotoUseCase)
        scopedOf(::UpdatePhotoFrameWidgetPhotoUseCase)
    }
}
