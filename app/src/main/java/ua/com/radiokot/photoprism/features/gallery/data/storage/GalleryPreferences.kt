package ua.com.radiokot.photoprism.features.gallery.data.storage

import io.reactivex.rxjava3.subjects.BehaviorSubject
import ua.com.radiokot.photoprism.features.gallery.data.model.GalleryItemScale

interface GalleryPreferences {
    val itemScale: BehaviorSubject<GalleryItemScale>
    var livePhotosAsImages: BehaviorSubject<Boolean>
}
