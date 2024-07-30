package ua.com.radiokot.photoprism.features.widgets.photoframe

import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.module.dsl.scopedOf
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module
import ua.com.radiokot.photoprism.di.APP_NO_BACKUP_PREFERENCES
import ua.com.radiokot.photoprism.env.data.model.EnvSession
import ua.com.radiokot.photoprism.features.gallery.data.model.GalleryMedia
import ua.com.radiokot.photoprism.features.widgets.photoframe.data.model.PhotoFrameWidgetShape
import ua.com.radiokot.photoprism.features.widgets.photoframe.data.storage.PhotoFrameWidgetPreferencesOnPrefs
import ua.com.radiokot.photoprism.features.widgets.photoframe.data.storage.PhotoFrameWidgetsPreferences
import ua.com.radiokot.photoprism.features.widgets.photoframe.logic.ReloadPhotoFrameWidgetPhotoUseCase
import ua.com.radiokot.photoprism.features.widgets.photoframe.logic.UpdatePhotoFrameWidgetPhotoUseCase
import ua.com.radiokot.photoprism.features.widgets.photoframe.view.model.PhotoFrameWidgetConfigurationViewModel

private const val ALLOWED_MEDIA_TYPES = "allowed-media-types"
private const val DEFAULT_SHAPE = "default-shape"

val photoFrameWidgetFeatureModule = module {
    single(named(ALLOWED_MEDIA_TYPES)) {
        setOf(
            GalleryMedia.TypeName.IMAGE,
            GalleryMedia.TypeName.RAW,
            GalleryMedia.TypeName.VECTOR,
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
            defaultShape = get(named(DEFAULT_SHAPE)),
        )
    } bind PhotoFrameWidgetsPreferences::class

    scope<EnvSession> {
        scopedOf(::ReloadPhotoFrameWidgetPhotoUseCase)

        scoped {
            UpdatePhotoFrameWidgetPhotoUseCase(
                allowedMediaTypes = get(named(ALLOWED_MEDIA_TYPES)),
                widgetsPreferences = get(),
                galleryMediaRepositoryFactory = get(),
            )
        }

        viewModel {
            PhotoFrameWidgetConfigurationViewModel(
                searchViewModel = get(),
                allowedMediaTypes = get(named(ALLOWED_MEDIA_TYPES)),
                workManager = get(),
                widgetsPreferences = get(),
            )
        }
    }
}
