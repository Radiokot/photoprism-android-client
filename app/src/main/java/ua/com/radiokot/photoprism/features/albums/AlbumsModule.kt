package ua.com.radiokot.photoprism.features.albums

import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module
import ua.com.radiokot.photoprism.di.APP_NO_BACKUP_PREFERENCES
import ua.com.radiokot.photoprism.env.data.model.EnvSession
import ua.com.radiokot.photoprism.features.albums.data.model.Album
import ua.com.radiokot.photoprism.features.albums.data.storage.AlbumsRepository
import ua.com.radiokot.photoprism.features.albums.view.model.AlbumSort
import ua.com.radiokot.photoprism.features.albums.view.model.AlbumsViewModel
import ua.com.radiokot.photoprism.features.gallery.folders.data.storage.GalleryAlbumsPreferences
import ua.com.radiokot.photoprism.features.gallery.folders.data.storage.GalleryAlbumsPreferencesOnPrefs
import ua.com.radiokot.photoprism.features.gallery.search.logic.SearchPredicates

val albumsModule = module {
    single {
        AlbumSort(
            order = AlbumSort.Order.NAME,
            areFavoritesFirst = true,
        )
    } bind AlbumSort::class

    single {
        GalleryAlbumsPreferencesOnPrefs(
            defaultSort = get(),
            preferences = get(named(APP_NO_BACKUP_PREFERENCES)),
            jsonObjectMapper = get(),
        )
    } bind GalleryAlbumsPreferences::class

    scope<EnvSession> {
        scoped {
            AlbumsRepository(
                types = setOf(
                    Album.TypeName.ALBUM,
                    Album.TypeName.FOLDER,
                ),
                defaultSort = get(),
                photoPrismAlbumsService = get(),
                previewUrlFactory = get(),
            )
        } bind AlbumsRepository::class

        viewModel {
            AlbumsViewModel(
                albumsRepository = get(),
                preferences = get(),
                searchPredicate = { album: Album, query: String ->
                    val fields = buildList {
                        add(album.title)
                        album.path
                            ?.split('/')
                            ?.filterNot(String::isEmpty)
                            ?.also(::addAll)
                    }

                    SearchPredicates.generalCondition(query, fields)
                },
            )
        }
    }
}
