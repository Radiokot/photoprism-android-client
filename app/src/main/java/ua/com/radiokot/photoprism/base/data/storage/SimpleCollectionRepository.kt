package ua.com.radiokot.photoprism.base.data.storage

import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.schedulers.Schedulers
import io.reactivex.rxjava3.subjects.BehaviorSubject
import io.reactivex.rxjava3.subjects.CompletableSubject

/**
 * Simple repository for a collection of type [T].
 * Use it if the data is mutable and you don't need caching (it doesn't have it).
 */
abstract class SimpleCollectionRepository<T> : Repository() {
    protected val itemsSubject = BehaviorSubject.createDefault(listOf<T>())

    /**
     * Emits all the collection items.
     */
    val items: Observable<List<T>> = itemsSubject

    open var itemsList: List<T> = emptyList()
        protected set

    protected abstract fun getCollection(): Single<List<T>>

    protected open fun onNewCollection(newItems: List<T>) {
        isNeverUpdated = false
        isFresh = true

        itemsList = newItems

        broadcast()
    }

    protected open fun broadcast() {
        itemsSubject.onNext(itemsList)
    }

    private var updateResultSubject: CompletableSubject? = null
    private var updateDisposable: Disposable? = null

    override fun update(): Completable = synchronized(this) {
        val resultSubject = updateResultSubject.let {
            if (it == null) {
                val new = CompletableSubject.create()
                updateResultSubject = new
                new
            } else {
                it
            }
        }

        isLoading = true

        updateDisposable?.dispose()
        updateDisposable = getCollection()
            .subscribeOn(Schedulers.io())
            .subscribeBy(
                onSuccess = { items ->
                    onNewCollection(items)

                    isLoading = false
                    updateResultSubject = null
                    resultSubject.onComplete()
                },
                onError = {
                    isLoading = false
                    errorsSubject.onNext(it)

                    updateResultSubject = null
                    resultSubject.onError(it)
                }
            )

        resultSubject
    }
}