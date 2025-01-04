package ua.com.radiokot.photoprism.features.gallery.data.storage

import io.reactivex.rxjava3.subjects.BehaviorSubject

interface DownloadPreferences {
    val downloadDirEn: BehaviorSubject<Boolean>
    val downloadDirName: BehaviorSubject<String>
}
