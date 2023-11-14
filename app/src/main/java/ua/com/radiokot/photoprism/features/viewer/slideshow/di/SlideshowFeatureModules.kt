package ua.com.radiokot.photoprism.features.viewer.slideshow.di

import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.module.Module
import org.koin.dsl.module
import ua.com.radiokot.photoprism.env.data.model.EnvSession
import ua.com.radiokot.photoprism.features.gallery.di.galleryFeatureModules
import ua.com.radiokot.photoprism.features.viewer.slideshow.view.model.SlideshowViewModel
import ua.com.radiokot.photoprism.features.viewer.di.mediaViewerFeatureModules

val slideshowFeatureModules: List<Module> = listOf(
    module {
        includes(mediaViewerFeatureModules)

        scope<EnvSession> {
            viewModel {
                SlideshowViewModel(
                    galleryMediaRepositoryFactory = get(),
                )
            }
        }
    }
)
