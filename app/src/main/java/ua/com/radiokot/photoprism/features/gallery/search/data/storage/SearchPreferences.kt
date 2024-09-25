package ua.com.radiokot.photoprism.features.gallery.search.data.storage

import io.reactivex.rxjava3.subjects.BehaviorSubject

interface SearchPreferences {
    val showPeople: BehaviorSubject<Boolean>
    val showAlbums: BehaviorSubject<Boolean>
    val showAlbumFolders: BehaviorSubject<Boolean>
}
