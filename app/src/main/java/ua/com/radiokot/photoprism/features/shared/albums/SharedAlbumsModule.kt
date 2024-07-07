package ua.com.radiokot.photoprism.features.shared.albums

import org.koin.dsl.bind
import org.koin.dsl.module
import ua.com.radiokot.photoprism.env.data.model.EnvSession
import ua.com.radiokot.photoprism.features.shared.albums.data.model.Album
import ua.com.radiokot.photoprism.features.shared.albums.data.storage.AlbumsRepository

val sharedAlbumsModule = module {
    scope<EnvSession> {
        scoped {
            AlbumsRepository(
                types = setOf(
                    Album.TypeName.ALBUM,
                    Album.TypeName.FOLDER,
                ),
                photoPrismAlbumsService = get(),
                previewUrlFactory = get(),
            )
        } bind AlbumsRepository::class
    }
}
