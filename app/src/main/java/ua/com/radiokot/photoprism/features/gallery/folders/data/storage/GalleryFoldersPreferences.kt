package ua.com.radiokot.photoprism.features.gallery.folders.data.storage

import io.reactivex.rxjava3.subjects.BehaviorSubject
import ua.com.radiokot.photoprism.features.shared.albums.view.model.AlbumSort

interface GalleryFoldersPreferences {
    val sort: BehaviorSubject<AlbumSort>
}
