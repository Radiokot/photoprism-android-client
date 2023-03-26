package ua.com.radiokot.photoprism.features.gallery.data.storage

import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.kotlin.toCompletable
import io.reactivex.rxjava3.schedulers.Schedulers
import ua.com.radiokot.photoprism.base.data.storage.SimpleCollectionRepository
import ua.com.radiokot.photoprism.extension.toSingle
import ua.com.radiokot.photoprism.features.gallery.data.model.SearchBookmark
import ua.com.radiokot.photoprism.features.gallery.data.model.SearchConfig

class SearchBookmarksRepository(
    private val bookmarksDbDao: SearchBookmarksDbDao,
) : SimpleCollectionRepository<SearchBookmark>() {

    override fun getCollection(): Single<List<SearchBookmark>> = {
        bookmarksDbDao.getAll()
            .map(::SearchBookmark)
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
                mutableItemsList.removeAt(index)
                mutableItemsList.add(index, bookmark)
                broadcast()
            }
        }

    fun create(
        name: String,
        searchConfig: SearchConfig,
    ): Single<SearchBookmark> = {
        val bookmark = SearchBookmark(
            id = System.currentTimeMillis(),
            name = name,
            searchConfig = searchConfig,
        )
        bookmarksDbDao.insert(bookmark.toDbEntity())
        bookmark
    }
        .toSingle()
        .subscribeOn(Schedulers.io())
        .doOnSuccess { newBookmark ->
            mutableItemsList.add(0, newBookmark)
            broadcast()
        }

    fun findByConfig(config: SearchConfig): SearchBookmark? =
        itemsList.find { it.searchConfig == config }
}