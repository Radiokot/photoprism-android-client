package ua.com.radiokot.photoprism.features.ext.memories.data.storage

import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.kotlin.toCompletable
import io.reactivex.rxjava3.schedulers.Schedulers
import ua.com.radiokot.photoprism.base.data.storage.SimpleCollectionRepository
import ua.com.radiokot.photoprism.extension.kLogger
import ua.com.radiokot.photoprism.extension.toSingle
import ua.com.radiokot.photoprism.features.ext.memories.data.model.Memory
import ua.com.radiokot.photoprism.features.ext.memories.data.model.MemoryDbEntity

class MemoriesRepository(
    private val memoriesDao: MemoriesDbDao,
) : SimpleCollectionRepository<Memory>() {
    private val log = kLogger("MemoriesRepo")

    override fun getCollection(): Single<List<Memory>> = {
        val maxCreatedAtMs = System.currentTimeMillis() - KEEP_MEMORIES_FOR_DAYS * 24 * 3600000L
        val deletedCount = memoriesDao.deleteExpired(maxCreatedAtMs)

        if (deletedCount > 0) {
            log.debug {
                "getCollection(): deleted_expired:" +
                        "\ndeletedCount=$deletedCount"
            }
        }

        memoriesDao
            .getAll()
            .map (MemoryDbEntity::toMemory)
    }.toSingle()

    fun add(newMemories: List<Memory>): Completable = {
        memoriesDao.insert(newMemories.map(::MemoryDbEntity))
    }
        .toCompletable()
        .subscribeOn(Schedulers.io())
        .andThen(updateDeferred())

    fun clear(): Completable = {
        memoriesDao.deleteAll()
    }
        .toCompletable()
        .subscribeOn(Schedulers.io())
        .doOnComplete {
            mutableItemsList.clear()
            broadcast()
        }

    fun markAsSeen(memory: Memory): Completable = {
        memoriesDao.markAsSeen(
            memorySearchQuery = memory.searchQuery,
        )
    }
        .toCompletable()
        .subscribeOn(Schedulers.io())
        .doOnComplete {
            memory.isSeen = true

            log.debug {
                "markAsSeen(): marked_memory_as_seen:" +
                        "\nmemory=$memory"
            }

            broadcast()
        }

    fun delete(memory: Memory): Completable = {
        memoriesDao.delete(
            memorySearchQuery = memory.searchQuery,
        )
    }
        .toCompletable()
        .subscribeOn(Schedulers.io())
        .doOnComplete {
            mutableItemsList.remove(memory)

            log.debug {
                "delete(): memory_deleted:" +
                        "\nmemory=$memory"
            }

            broadcast()
        }

    private companion object {
        private const val KEEP_MEMORIES_FOR_DAYS = 2
    }
}
