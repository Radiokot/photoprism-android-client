package ua.com.radiokot.photoprism.features.gallery.data.storage

import io.reactivex.rxjava3.subjects.BehaviorSubject
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.subjects.PublishSubject
import ua.com.radiokot.photoprism.features.gallery.data.model.RawSharingMode
import ua.com.radiokot.photoprism.features.gallery.view.model.GalleryItemScale
import ua.com.radiokot.photoprism.features.gallery.data.model.GalleryItemsOrder

interface GalleryPreferences {
    val itemScale: BehaviorSubject<GalleryItemScale>
    val livePhotosAsImages: BehaviorSubject<Boolean>
    val rawSharingMode: BehaviorSubject<RawSharingMode>
    val cacheSizeLimitInMb: BehaviorSubject<Int>
    val clearCache: PublishSubject<Unit>;
    fun getItemsOrderBySearchQuery(searchQuery: String?): BehaviorSubject<GalleryItemsOrder>
}


