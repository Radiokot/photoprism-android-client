package ua.com.radiokot.photoprism.features.gallery.data.storage

import io.reactivex.rxjava3.subjects.BehaviorSubject
import ua.com.radiokot.photoprism.features.gallery.data.model.RawSharingMode
import ua.com.radiokot.photoprism.features.gallery.view.model.GalleryItemScale
import ua.com.radiokot.photoprism.features.gallery.data.model.GalleryItemsOrder

interface GalleryPreferences {
    val itemScale: BehaviorSubject<GalleryItemScale>
    val livePhotosAsImages: BehaviorSubject<Boolean>
    val rawSharingMode: BehaviorSubject<RawSharingMode>

    fun getItemsOrderBySearchQuery(searchQuery: String?): BehaviorSubject<GalleryItemsOrder>
}
