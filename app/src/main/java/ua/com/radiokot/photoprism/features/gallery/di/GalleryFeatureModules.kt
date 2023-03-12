package ua.com.radiokot.photoprism.features.gallery.di

import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.module.Module
import org.koin.dsl.bind
import org.koin.dsl.module
import ua.com.radiokot.photoprism.api.PhotoPrismSession
import ua.com.radiokot.photoprism.features.gallery.data.storage.SimpleGalleryMediaRepository
import ua.com.radiokot.photoprism.features.gallery.logic.MediaThumbnailUrlFactory
import ua.com.radiokot.photoprism.features.gallery.logic.PhotoPrismThumbnailUrlFactory
import ua.com.radiokot.photoprism.features.gallery.view.GalleryViewModel

val galleryFeatureModules: List<Module> = listOf(
    module {
        scope<PhotoPrismSession> {
            scoped {
                val session = get<PhotoPrismSession>()

                PhotoPrismThumbnailUrlFactory(
                    apiUrl = getProperty("apiUrl"),
                    previewToken = session.previewToken,
                )
            }.bind(MediaThumbnailUrlFactory::class)

            scoped {
                SimpleGalleryMediaRepository(
                    photoPrismPhotosService = get(),
                    thumbnailUrlFactory = get(),
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