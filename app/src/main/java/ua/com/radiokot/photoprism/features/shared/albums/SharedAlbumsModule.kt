package ua.com.radiokot.photoprism.features.shared.albums

import org.koin.dsl.bind
import org.koin.dsl.module
import ua.com.radiokot.photoprism.env.data.model.EnvSession
import ua.com.radiokot.photoprism.extension.autoDispose
import ua.com.radiokot.photoprism.features.gallery.data.storage.SearchPreferences
import ua.com.radiokot.photoprism.features.shared.albums.data.storage.AlbumsRepository

val sharedAlbumsModule = module {
    scope<EnvSession> {
        scoped {
            AlbumsRepository(
                photoPrismAlbumsService = get(),
                previewUrlFactory = get(),
            ).also { repository ->
                val searchPreferences = get<SearchPreferences>()

                // Subscribe the repo to the folders preference.
                searchPreferences
                    .showAlbumFolders
                    .subscribe(repository::includeFolders::set)
                    .autoDispose(this@scoped)
            }
        } bind AlbumsRepository::class
    }
}
