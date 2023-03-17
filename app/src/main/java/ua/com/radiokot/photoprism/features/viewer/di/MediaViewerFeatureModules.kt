package ua.com.radiokot.photoprism.features.viewer.di

import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.module.Module
import org.koin.dsl.module
import ua.com.radiokot.photoprism.features.env.data.model.PhotoPrismSession
import ua.com.radiokot.photoprism.features.gallery.di.galleryFeatureModules
import ua.com.radiokot.photoprism.features.viewer.view.model.MediaViewerViewModel

val mediaViewerFeatureModules: List<Module> = listOf(
    module {
        includes(galleryFeatureModules)

        scope<PhotoPrismSession> {
            viewModel {
                MediaViewerViewModel(
                    galleryMediaRepositoryFactory = get(),
                )
            }
        }
    }
)