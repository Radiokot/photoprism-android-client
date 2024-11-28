package ua.com.radiokot.photoprism.features.albums

import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module
import ua.com.radiokot.photoprism.di.APP_NO_BACKUP_PREFERENCES
import ua.com.radiokot.photoprism.env.data.model.EnvSession
import ua.com.radiokot.photoprism.features.albums.data.model.Album
import ua.com.radiokot.photoprism.features.albums.data.model.DestinationAlbum
import ua.com.radiokot.photoprism.features.albums.data.storage.AlbumsPreferences
import ua.com.radiokot.photoprism.features.albums.data.storage.AlbumsPreferencesOnPrefs
import ua.com.radiokot.photoprism.features.albums.data.storage.AlbumsRepository
import ua.com.radiokot.photoprism.features.albums.view.model.AlbumSort
import ua.com.radiokot.photoprism.features.albums.view.model.AlbumsViewModel
import ua.com.radiokot.photoprism.features.albums.view.model.DestinationAlbumSelectionViewModel
import ua.com.radiokot.photoprism.features.gallery.search.logic.SearchPredicates

val albumsFeatureModule = module {
    single {
        AlbumSort(
            order = AlbumSort.Order.NAME,
            areFavoritesFirst = true,
        )
    } bind AlbumSort::class

    single {
        AlbumsPreferencesOnPrefs(
            defaultSort = get(),
            defaultMonthSort = AlbumSort(
                order = AlbumSort.Order.NEWEST_FIRST,
                areFavoritesFirst = false,
            ),
            preferences = get(named(APP_NO_BACKUP_PREFERENCES)),
            jsonObjectMapper = get(),
        )
    } bind AlbumsPreferences::class

    scope<EnvSession> {
        scoped {
            AlbumsRepository.Factory(
                photoPrismAlbumsService = get(),
            )
        } bind AlbumsRepository.Factory::class

        factory {
            get<AlbumsRepository.Factory>().albums
        } bind AlbumsRepository::class

        viewModel {
            AlbumsViewModel(
                albumsRepositoryFactory = get(),
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
                previewUrlFactory = get(),
            )
        }

        viewModel {
            DestinationAlbumSelectionViewModel(
                albumsRepository = get(),
                preferences = get(),
                searchPredicate = { album: DestinationAlbum, query: String ->
                    SearchPredicates.generalCondition(query, album.title)
                },
                exactMatchPredicate = { album: DestinationAlbum, query: String ->
                    album.title.equals(query, ignoreCase = true)
                },
            )
        }
    }
}
