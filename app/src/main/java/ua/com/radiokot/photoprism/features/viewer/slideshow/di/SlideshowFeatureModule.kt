package ua.com.radiokot.photoprism.features.viewer.slideshow.di

import org.koin.androidx.viewmodel.dsl.viewModelOf
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module
import ua.com.radiokot.photoprism.di.APP_NO_BACKUP_PREFERENCES
import ua.com.radiokot.photoprism.env.data.model.EnvSession
import ua.com.radiokot.photoprism.features.viewer.mediaViewerFeatureModule
import ua.com.radiokot.photoprism.features.viewer.slideshow.data.storage.SlideshowPreferences
import ua.com.radiokot.photoprism.features.viewer.slideshow.data.storage.SlideshowPreferencesOnPrefs
import ua.com.radiokot.photoprism.features.viewer.slideshow.view.model.SlideshowViewModel

val slideshowFeatureModule = module {
    includes(mediaViewerFeatureModule)

    single {
        SlideshowPreferencesOnPrefs(
            preferences = get(named(APP_NO_BACKUP_PREFERENCES)),
            keyPrefix = "slideshow"
        )
    } bind SlideshowPreferences::class

    scope<EnvSession> {
        viewModelOf(::SlideshowViewModel)
    }
}
