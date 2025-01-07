package ua.com.radiokot.photoprism.features.gallery.data.storage

import io.reactivex.rxjava3.subjects.BehaviorSubject

interface DownloadPreferences {
    val useSeparateFolder: BehaviorSubject<Boolean>
    val separateFolderName: BehaviorSubject<String>
}
