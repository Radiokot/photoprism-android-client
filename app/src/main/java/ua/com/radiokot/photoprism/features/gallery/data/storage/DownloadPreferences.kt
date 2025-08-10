package ua.com.radiokot.photoprism.features.gallery.data.storage

import io.reactivex.rxjava3.subjects.BehaviorSubject
import ua.com.radiokot.photoprism.features.gallery.data.model.RawDownloadMode

interface DownloadPreferences {
    val useSeparateFolder: BehaviorSubject<Boolean>
    val separateFolderName: BehaviorSubject<String>
    val rawDownloadMode: BehaviorSubject<RawDownloadMode>
}
