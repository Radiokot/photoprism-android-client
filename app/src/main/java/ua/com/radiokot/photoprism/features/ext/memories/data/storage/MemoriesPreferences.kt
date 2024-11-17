package ua.com.radiokot.photoprism.features.ext.memories.data.storage

import io.reactivex.rxjava3.subjects.BehaviorSubject

interface MemoriesPreferences {
    val isEnabled: BehaviorSubject<Boolean>
    /**
     * Preference for devices with SDK < 26
     * where notification channels are not available.
     */
    var areNotificationsEnabled: Boolean
}
