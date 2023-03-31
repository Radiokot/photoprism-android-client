package ua.com.radiokot.photoprism.util

import io.reactivex.rxjava3.core.Single
import ua.com.radiokot.photoprism.base.data.model.DataPage

typealias PageProvider<T> = (
    cursor: String?,
) -> Single<DataPage<T>>

/**
 * Loads a paged collection in a single shot.
 *
 * @param pageProvider provider of the next page loading [Single]
 * @param startCursor cursor to start from
 * @param distinct if set to true, no duplicates will be added to the result.
 * Useful when loading a mutable collection in the descending order with page-number-based pagination.
 *
 * @see loadAll
 */
class PagedCollectionLoader<T>(
    private val pageProvider: PageProvider<T>,
    private val startCursor: String? = null,
    private val distinct: Boolean = true
) {
    fun loadAll(): Single<List<T>> {
        var cursor = startCursor
        var reachedLastPage = false

        return Single.defer {
            pageProvider(cursor)
        }
            .doOnSuccess { page ->
                cursor = page.nextCursor
                reachedLastPage = page.isLast
            }
            .repeatUntil { reachedLastPage }
            .collect<MutableCollection<T>>(
                {
                    if (distinct)
                        linkedSetOf()
                    else
                        mutableListOf()
                },
                { collection, page ->
                    collection.addAll(page.items)
                }
            )
            .map(MutableCollection<T>::toList)
    }
}