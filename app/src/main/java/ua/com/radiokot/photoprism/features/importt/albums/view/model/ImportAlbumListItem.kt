package ua.com.radiokot.photoprism.features.importt.albums.view.model

import ua.com.radiokot.photoprism.features.importt.albums.data.model.ImportAlbum

sealed interface ImportAlbumListItem {
    class CreateNew(
        val newAlbumTitle: String,
    ) : ImportAlbumListItem

    class Album(
        val title: String,
        val isAlbumSelected: Boolean,
        val source: ImportAlbum?,
    ) : ImportAlbumListItem {

        constructor(
            source: ImportAlbum,
            isAlbumSelected: Boolean,
        ) : this(
            title = source.title,
            isAlbumSelected = isAlbumSelected,
            source = source,
        )
    }
}
