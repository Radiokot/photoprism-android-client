package ua.com.radiokot.photoprism.features.albums.data.storage

import io.reactivex.rxjava3.subjects.BehaviorSubject
import ua.com.radiokot.photoprism.features.albums.view.model.AlbumSort

interface AlbumsPreferences {
    val folderSort: BehaviorSubject<AlbumSort>
    val albumSort: BehaviorSubject<AlbumSort>
}
