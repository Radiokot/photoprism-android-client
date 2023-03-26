package ua.com.radiokot.photoprism.base.data.storage

import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.schedulers.Schedulers
import io.reactivex.rxjava3.subjects.BehaviorSubject
import io.reactivex.rxjava3.subjects.CompletableSubject
import ua.com.radiokot.photoprism.base.data.model.DataPage
import ua.com.radiokot.photoprism.base.data.model.PagingOrder

/**
 * Simple repository for paged data of type [T].
 * Use it if the data is mutable and you don't need caching (it doesn't have it).
 * Works with both cursor and page-number pagination
 */
abstract class SimplePagedDataRepository<T>(
    protected open val pagingOrder: PagingOrder,
    protected open val pageLimit: Int,
) : Repository() {
    private var nextPage: String? = null

    protected val itemsSubject = BehaviorSubject.createDefault(listOf<T>())
    protected open val mutableItemsList = mutableListOf<T>()

    /**
     * Emits all the currently loaded items.
     */
    val items: Observable<List<T>> = itemsSubject

    open val itemsList: List<T>
        get() = itemsSubject.value ?: listOf()

    val isOnFirstPage: Boolean
        get() = nextPage == null

    var noMoreItems: Boolean = false
        protected set

    /**
     * @param cursor - cursor or number of the page to load
     */
    protected abstract fun getPage(
        limit: Int,
        cursor: String?,
        order: PagingOrder
    ): Single<DataPage<T>>

    protected var loadingDisposable: Disposable? = null
    protected open fun loadMore(
        force: Boolean,
        resultSubject: CompletableSubject?
    ): Boolean {
        synchronized(this) {
            if ((noMoreItems || isLoading) && !force) {
                return false
            }

            isLoading = true

            loadingDisposable?.dispose()
            loadingDisposable = getPage(pageLimit, nextPage, pagingOrder)
                .subscribeOn(Schedulers.io())
                .subscribeBy(
                    onSuccess = {
                        onNewPage(it)

                        isLoading = false

                        updateResultSubject = null
                        resultSubject?.onComplete()
                    },
                    onError = {
                        isLoading = false
                        errorsSubject.onNext(it)

                        updateResultSubject = null
                        resultSubject?.onError(it)
                    }
                )
        }
        return true
    }

    open fun onNewPage(page: DataPage<T>) {
        isNeverUpdated = false
        noMoreItems = page.isLast

        if (pagingOrder == PagingOrder.DESC && isOnFirstPage
            || pagingOrder == PagingOrder.ASC && noMoreItems
        ) {
            isFresh = true
        }

        nextPage = page.nextCursor

        mutableItemsList.addAll(page.items)

        broadcast()
    }

    open fun loadMore(): Boolean {
        return loadMore(force = false, resultSubject = null)
    }

    private var updateResultSubject: CompletableSubject? = null

    override fun update(): Completable = synchronized(this) {
        mutableItemsList.clear()
        nextPage = null
        noMoreItems = false

        isLoading = false

        val resultSubject = updateResultSubject.let {
            if (it == null) {
                val new = CompletableSubject.create()
                updateResultSubject = new
                new
            } else {
                it
            }
        }

        loadMore(force = true, resultSubject = resultSubject)

        resultSubject
    }

    protected open fun broadcast() {
        itemsSubject.onNext(mutableItemsList.toList())
    }
}