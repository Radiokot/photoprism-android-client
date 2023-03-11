package ua.com.radiokot.photoprism.features.gallery.di

import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.module.Module
import org.koin.dsl.module
import ua.com.radiokot.photoprism.api.PhotoPrismSession
import ua.com.radiokot.photoprism.features.gallery.data.storage.SimpleGalleryMediaRepository
import ua.com.radiokot.photoprism.features.gallery.view.GalleryViewModel

val galleryFeatureModules: List<Module> = listOf(
    module {
        scope<PhotoPrismSession> {
            scoped {
                SimpleGalleryMediaRepository(
                    photoPrismPhotosService = get(),
                    pageLimit = 50,
                )
            }
        }
    },

    module {
        scope<PhotoPrismSession> {
            viewModel {
                GalleryViewModel(
                    galleryMediaRepository = get(),
                )
            }
        }
    }
)