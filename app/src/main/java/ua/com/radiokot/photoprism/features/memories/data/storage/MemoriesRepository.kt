package ua.com.radiokot.photoprism.features.memories.data.storage

import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.kotlin.toCompletable
import io.reactivex.rxjava3.schedulers.Schedulers
import ua.com.radiokot.photoprism.base.data.storage.SimpleCollectionRepository
import ua.com.radiokot.photoprism.features.memories.data.model.Memory

class MemoriesRepository : SimpleCollectionRepository<Memory>() {
    // TODO: Replace with a persistent storage.
    private val tempStorage: MutableSet<Memory> = mutableSetOf()

    override fun getCollection(): Single<List<Memory>> =
        Single.just(
            tempStorage
                .toList()
                .sortedDescending()
        )

    fun add(newMemories: List<Memory>): Completable = {
        tempStorage
            .addAll(newMemories)

        // TODO: Add cleanup of old seen memories.
    }
        .toCompletable()
        .subscribeOn(Schedulers.io())
        .doOnComplete(::broadcast)
}
