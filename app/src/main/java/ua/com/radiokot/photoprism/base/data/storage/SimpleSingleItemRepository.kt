package ua.com.radiokot.photoprism.base.data.storage

import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.schedulers.Schedulers
import io.reactivex.rxjava3.subjects.BehaviorSubject
import io.reactivex.rxjava3.subjects.CompletableSubject

abstract class SimpleSingleItemRepository<T : Any> : Repository() {
    protected val itemSubject: BehaviorSubject<T> = BehaviorSubject.create()
    val item: Observable<T> = itemSubject

    protected abstract fun getItem(): Single<T>

    protected open fun onNewItem(newItem: T) {
        isNeverUpdated = false
        isFresh = true
        itemSubject.onNext(newItem)
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
        updateDisposable = getItem()
            .subscribeOn(Schedulers.io())
            .subscribeBy(
                onSuccess = { item ->
                    onNewItem(item)

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
