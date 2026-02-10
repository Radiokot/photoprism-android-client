package ua.com.radiokot.photoprism.features.gallery.data.storage

import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.kotlin.toCompletable
import io.reactivex.rxjava3.schedulers.Schedulers
import ua.com.radiokot.photoprism.base.data.storage.SimpleCollectionRepository
import ua.com.radiokot.photoprism.extension.kLogger
import ua.com.radiokot.photoprism.extension.toSingle
import ua.com.radiokot.photoprism.features.gallery.data.model.SearchBookmark
import ua.com.radiokot.photoprism.features.gallery.data.model.SearchConfig
import ua.com.radiokot.photoprism.util.SternBrocotTreeSearch

class SearchBookmarksRepository(
    private val bookmarksDbDao: SearchBookmarksDbDao,
) : SimpleCollectionRepository<SearchBookmark>() {
    private val log = kLogger("SearchBookmarksRepo")

    override fun getCollection(): Single<List<SearchBookmark>> = {
        bookmarksDbDao.getAll()
            .map(::SearchBookmark)
            .also { bookmarks ->
                log.debug {
                    "getCollection(): bookmark_positions:" +
                            "\npositions=${bookmarks.map { "${it.id}:${it.position}" }}"
                }
            }
    }.toSingle()

    fun delete(bookmark: SearchBookmark): Completable = {
        bookmarksDbDao.delete(bookmark.id)
    }
        .toCompletable()
        .subscribeOn(Schedulers.io())
        .doOnComplete {
            mutableItemsList.remove(bookmark)
            broadcast()
        }

    fun update(bookmark: SearchBookmark): Completable = {
        bookmarksDbDao.update(bookmark.toDbEntity())
    }
        .toCompletable()
        .subscribeOn(Schedulers.io())
        .doOnComplete {
            val index = mutableItemsList.indexOf(bookmark)
            if (index != -1) {
                val currentCopy = mutableItemsList[index]
                mutableItemsList.removeAt(index)
                if (currentCopy.position != bookmark.position) {
                    mutableItemsList.add(bookmark)
                    mutableItemsList.sort()
                } else {
                    mutableItemsList.add(index, bookmark)
                }
                broadcast()
            }
        }

    fun create(
        name: String,
        searchConfig: SearchConfig,
    ): Single<SearchBookmark> = {
        // Insert new bookmarks to the top.
        val position = getStartPosition(
            currentMinPosition = bookmarksDbDao.getMinPosition()
        )

        SearchBookmark(
            id = System.currentTimeMillis(),
            position = position,
            name = name,
            searchConfig = searchConfig,
        ).also { bookmarksDbDao.insert(it.toDbEntity()) }
    }
        .toSingle()
        .subscribeOn(Schedulers.io())
        .doOnSuccess { newBookmark ->
            mutableItemsList.add(0, newBookmark)
            broadcast()
        }

    private fun getStartPosition(currentMinPosition: Double?): Double =
        getPosition(
            after = 0.0,
            before = currentMinPosition
        )

    private fun getPosition(after: Double, before: Double?): Double =
        SternBrocotTreeSearch()
            .goBetween(
                lowerBound = after,
                upperBound = before ?: Double.POSITIVE_INFINITY
            )
            .value

    /**
     * Moves given [bookmark] next to the bookmark with given [id]
     *
     * @param id ID of the bookmark to place next to, or null when moving to the start
     */
    fun placeAfter(
        id: Long?,
        bookmark: SearchBookmark
    ): Completable = {
        val newPosition: Double

        if (id == null) {
            newPosition = getStartPosition(
                currentMinPosition = bookmarksDbDao.getMinPosition()
            )

            log.debug {
                "placeAfter(): placing_at_the_start:" +
                        "\nnewPosition=$newPosition"
            }
        } else {
            val idAndNextPositions = bookmarksDbDao.getIdPositionAndNext(id)
            newPosition = getPosition(
                after = idAndNextPositions[0],
                before = idAndNextPositions.getOrNull(1)
            )

            log.debug {
                "placeAfter(): placing_after:" +
                        "\npreviousId=$id," +
                        "\nidAndNextPositions=$idAndNextPositions," +
                        "\nnewPosition=$newPosition"
            }
        }

        newPosition
    }
        .toSingle()
        .subscribeOn(Schedulers.io())
        .flatMapCompletable { newPosition ->
            update(bookmark.copy(position = newPosition))
        }

    fun swapPositions(
        first: SearchBookmark,
        second: SearchBookmark,
    ): Completable =
        update(first.copy(position = second.position))
            .andThen(update(second.copy(position = first.position)))

    fun findByConfig(config: SearchConfig): SearchBookmark? =
        itemsList.find { it.searchConfig == config }

    fun findById(id: Long): SearchBookmark? =
        itemsList.find { it.id == id }

    /**
     * Replaces all the bookmarks with given.
     */
    fun import(bookmarks: List<SearchBookmark>): Completable = {
        invalidate()

        bookmarksDbDao.deleteAll()
        bookmarksDbDao.insert(*bookmarks.map(SearchBookmark::toDbEntity).toTypedArray())
    }
        .toCompletable()
        .subscribeOn(Schedulers.io())
        .andThen(updateDeferred())
}
