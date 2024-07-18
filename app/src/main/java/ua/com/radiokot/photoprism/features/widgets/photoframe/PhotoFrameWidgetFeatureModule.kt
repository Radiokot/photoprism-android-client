package ua.com.radiokot.photoprism.features.widgets.photoframe

import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module
import ua.com.radiokot.photoprism.di.APP_NO_BACKUP_PREFERENCES
import ua.com.radiokot.photoprism.features.widgets.photoframe.data.storage.PhotoFrameWidgetPreferencesOnPrefs
import ua.com.radiokot.photoprism.features.widgets.photoframe.data.storage.PhotoFrameWidgetsPreferences

val photoFrameWidgetFeatureModule = module {
    single {
      PhotoFrameWidgetPreferencesOnPrefs(
          preferences = get(named(APP_NO_BACKUP_PREFERENCES)),
          keyPrefix = "photo_frame_widget",
      )
    } bind PhotoFrameWidgetsPreferences::class
}
