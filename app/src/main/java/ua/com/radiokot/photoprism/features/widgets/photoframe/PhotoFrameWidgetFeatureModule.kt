package ua.com.radiokot.photoprism.features.widgets.photoframe

import org.koin.android.ext.koin.androidApplication
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.module.dsl.singleOf
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module
import ua.com.radiokot.photoprism.di.APP_NO_BACKUP_PREFERENCES
import ua.com.radiokot.photoprism.di.UTC_DAY_YEAR_SHORT_DATE_FORMAT
import ua.com.radiokot.photoprism.env.data.model.EnvSession
import ua.com.radiokot.photoprism.features.gallery.data.model.GalleryMedia
import ua.com.radiokot.photoprism.features.widgets.photoframe.data.model.PhotoFrameWidgetShape
import ua.com.radiokot.photoprism.features.widgets.photoframe.data.storage.PhotoFrameWidgetPreferencesOnPrefs
import ua.com.radiokot.photoprism.features.widgets.photoframe.data.storage.PhotoFrameWidgetsPreferences
import ua.com.radiokot.photoprism.features.widgets.photoframe.logic.ReloadPhotoFrameWidgetPhotoUseCase
import ua.com.radiokot.photoprism.features.widgets.photoframe.logic.UpdatePhotoFrameWidgetManifestComponentsUseCase
import ua.com.radiokot.photoprism.features.widgets.photoframe.logic.UpdatePhotoFrameWidgetPhotoUseCase
import ua.com.radiokot.photoprism.features.widgets.photoframe.view.model.PhotoFrameWidgetConfigurationViewModel

private const val ALLOWED_MEDIA_TYPES = "allowed-media-types"

val photoFrameWidgetFeatureModule = module {
    single(named(ALLOWED_MEDIA_TYPES)) {
        setOf(
            GalleryMedia.TypeName.IMAGE,
            GalleryMedia.TypeName.RAW,
            GalleryMedia.TypeName.VECTOR,
            GalleryMedia.TypeName.LIVE,
        )
    }

    single {
        PhotoFrameWidgetPreferencesOnPrefs(
            keyPrefix = "photo_frame_widget",
            preferences = get(named(APP_NO_BACKUP_PREFERENCES)),
            jsonObjectMapper = get(),
            defaultShape = PhotoFrameWidgetShape.ROUNDED_CORNERS,
        )
    } bind PhotoFrameWidgetsPreferences::class

    singleOf(::UpdatePhotoFrameWidgetManifestComponentsUseCase)

    scope<EnvSession> {
        scoped {
            ReloadPhotoFrameWidgetPhotoUseCase(
                picasso = get(),
                widgetsPreferences = get(),
                dayYearShortDateFormat = get(named(UTC_DAY_YEAR_SHORT_DATE_FORMAT)),
                context = androidApplication(),
            )
        } bind ReloadPhotoFrameWidgetPhotoUseCase::class

        scoped {
            UpdatePhotoFrameWidgetPhotoUseCase(
                allowedMediaTypes = get(named(ALLOWED_MEDIA_TYPES)),
                widgetsPreferences = get(),
                galleryMediaRepositoryFactory = get(),
                photoPrismPhotosService = get(),
                previewUrlFactory = get(),
            )
        } bind UpdatePhotoFrameWidgetPhotoUseCase::class

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
