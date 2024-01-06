package ua.com.radiokot.photoprism.features.memories.data.storage

import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.kotlin.toCompletable
import io.reactivex.rxjava3.schedulers.Schedulers
import ua.com.radiokot.photoprism.base.data.storage.SimpleCollectionRepository
import ua.com.radiokot.photoprism.features.memories.data.model.Memory

class MemoriesRepository : SimpleCollectionRepository<Memory>() {
    // TODO: Replace with a persistent storage.
    override fun getCollection(): Single<List<Memory>> =
        Single.just(
            mutableItemsList
                .toList()
                .sortedDescending()
        )

    fun add(newMemories: List<Memory>): Completable = {
        mutableItemsList
            .addAll(newMemories)

        // TODO: Add cleanup of old seen memories.
    }
        .toCompletable()
        .subscribeOn(Schedulers.io())
        .doOnComplete(::broadcast)
}
