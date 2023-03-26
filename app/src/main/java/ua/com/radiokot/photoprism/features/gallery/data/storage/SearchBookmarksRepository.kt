package ua.com.radiokot.photoprism.features.gallery.data.storage

import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.kotlin.toCompletable
import io.reactivex.rxjava3.schedulers.Schedulers
import ua.com.radiokot.photoprism.base.data.storage.SimpleCollectionRepository
import ua.com.radiokot.photoprism.extension.toSingle
import ua.com.radiokot.photoprism.features.gallery.data.model.SearchBookmark
import ua.com.radiokot.photoprism.features.gallery.data.model.SearchConfig

class SearchBookmarksRepository : SimpleCollectionRepository<SearchBookmark>() {
    override fun getCollection(): Single<List<SearchBookmark>> = {
        var i = 0L
        listOf(
            SearchBookmark(
                name = "My Screenshots",
                id = ++i,
                searchConfig = SearchConfig(
                    userQuery = "oleg&screen",
                    mediaTypes = emptySet(),
                )
            ),
            SearchBookmark(
                name = "Yasya Camera",
                id = ++i,
                searchConfig = SearchConfig(
                    userQuery = "yasya name:\"IMG_*\"|\"VID_*\"",
                    mediaTypes = emptySet(),
                )
            ),
            SearchBookmark(
                name = "My Camera",
                id = ++i,
                searchConfig = SearchConfig(
                    userQuery = "oleg&camera|cam",
                    mediaTypes = emptySet(),
                )
            ),
        )
    }.toSingle()

    fun delete(bookmark: SearchBookmark): Completable = {
        mutableItemsList.remove(bookmark)
        broadcast()
    }.toCompletable().subscribeOn(Schedulers.io())

    fun update(bookmark: SearchBookmark): Completable = {
        val index = mutableItemsList.indexOf(bookmark)
        check(index >= 0)
        mutableItemsList.removeAt(index)
        mutableItemsList.add(index, bookmark)
        broadcast()
    }.toCompletable().subscribeOn(Schedulers.io())

//    fun create(name: String): Single<SearchBookmark> = {
//        SearchBookmark(
//            id = System.currentTimeMillis(),
//            name = name,
//        ).also { mutableItemsList.add(0, it) }
//    }
//        .toSingle()
//        .subscribeOn(Schedulers.io())
//        .doOnSuccess { broadcast() }
}