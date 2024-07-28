package ua.com.radiokot.photoprism.features.widgets.photoframe

import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.module.dsl.scopedOf
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module
import ua.com.radiokot.photoprism.di.APP_NO_BACKUP_PREFERENCES
import ua.com.radiokot.photoprism.env.data.model.EnvSession
import ua.com.radiokot.photoprism.features.gallery.data.model.GalleryMedia
import ua.com.radiokot.photoprism.features.gallery.data.model.SearchConfig
import ua.com.radiokot.photoprism.features.widgets.photoframe.data.model.PhotoFrameWidgetShape
import ua.com.radiokot.photoprism.features.widgets.photoframe.data.storage.PhotoFrameWidgetPreferencesOnPrefs
import ua.com.radiokot.photoprism.features.widgets.photoframe.data.storage.PhotoFrameWidgetsPreferences
import ua.com.radiokot.photoprism.features.widgets.photoframe.logic.ReloadPhotoFrameWidgetPhotoUseCase
import ua.com.radiokot.photoprism.features.widgets.photoframe.logic.UpdatePhotoFrameWidgetPhotoUseCase
import ua.com.radiokot.photoprism.features.widgets.photoframe.view.model.PhotoFrameWidgetConfigurationViewModel

private const val DEFAULT_SEARCH_CONFIG = "default-search-config"
private const val DEFAULT_SHAPE = "default-shape"

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

    single(named(DEFAULT_SHAPE)) {
        PhotoFrameWidgetShape.ROUNDED_CORNERS
    } bind PhotoFrameWidgetShape::class

    single {
        PhotoFrameWidgetPreferencesOnPrefs(
            keyPrefix = "photo_frame_widget",
            preferences = get(named(APP_NO_BACKUP_PREFERENCES)),
            jsonObjectMapper = get(),
            defaultSearchConfig = get(named(DEFAULT_SEARCH_CONFIG)),
            defaultShape =  get(named(DEFAULT_SHAPE)),
        )
    } bind PhotoFrameWidgetsPreferences::class

    scope<EnvSession> {
        scopedOf(::ReloadPhotoFrameWidgetPhotoUseCase)
        scopedOf(::UpdatePhotoFrameWidgetPhotoUseCase)

        viewModel {
            PhotoFrameWidgetConfigurationViewModel(
                searchViewModel = get(),
                defaultSearchConfig = get(named(DEFAULT_SEARCH_CONFIG)),
                defaultShape = get(named(DEFAULT_SHAPE)),
            )
        }
    }
}
