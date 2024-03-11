package ua.com.radiokot.photoprism.features.ext.memories.data.storage

interface MemoriesPreferences {
    /**
     * Preference for devices with SDK < 26
     * where notification channels are not available.
     */
    var areNotificationsEnabled: Boolean
}
