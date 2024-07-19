package ua.com.radiokot.photoprism.features.widgets.photoframe

import org.koin.core.module.dsl.scopedOf
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module
import ua.com.radiokot.photoprism.di.APP_NO_BACKUP_PREFERENCES
import ua.com.radiokot.photoprism.env.data.model.EnvSession
import ua.com.radiokot.photoprism.features.widgets.photoframe.data.storage.PhotoFrameWidgetPreferencesOnPrefs
import ua.com.radiokot.photoprism.features.widgets.photoframe.data.storage.PhotoFrameWidgetsPreferences
import ua.com.radiokot.photoprism.features.widgets.photoframe.logic.ReloadPhotoFrameWidgetPhotoUseCase

val photoFrameWidgetFeatureModule = module {
    scope<EnvSession> {
        scopedOf(::ReloadPhotoFrameWidgetPhotoUseCase)
    }

    single {
        PhotoFrameWidgetPreferencesOnPrefs(
            preferences = get(named(APP_NO_BACKUP_PREFERENCES)),
            keyPrefix = "photo_frame_widget",
        )
    } bind PhotoFrameWidgetsPreferences::class
}
