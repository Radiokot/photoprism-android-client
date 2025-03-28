package ua.com.radiokot.photoprism.features.ext.memories.data.storage

import io.reactivex.rxjava3.subjects.BehaviorSubject
import ua.com.radiokot.photoprism.features.people.data.model.Person

interface MemoriesPreferences {
    val isEnabled: BehaviorSubject<Boolean>
    var maxEntriesInMemory: Int

    /**
     * Preference for devices with SDK < 26
     * where notification channels are not available.
     */
    var areNotificationsEnabled: Boolean

    /**
     * A set of [Person.id] the user preferred to forget,
     * therefore not to see in memories 😟.
     */
    var personIdsToForget: Set<String>
}
