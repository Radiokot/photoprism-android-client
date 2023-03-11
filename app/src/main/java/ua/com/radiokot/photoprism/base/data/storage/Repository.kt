package ua.com.radiokot.photoprism.base.data.storage

import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.subjects.BehaviorSubject
import io.reactivex.rxjava3.subjects.PublishSubject
import java.util.concurrent.TimeUnit

/**
 * Contains common repository logic. Is a parent of all repositories.
 */
abstract class Repository {
    protected val errorsSubject: PublishSubject<Throwable> =
        PublishSubject.create()

    /**
     * Emits repository errors.
     */
    val errors: Observable<Throwable> = errorsSubject

    protected open val loadingSubject: BehaviorSubject<Boolean> =
        BehaviorSubject.createDefault(false)

    /**
     * Emits repository loading states.
     * @see Repository.isLoading
     */
    val loading: Observable<Boolean> by lazy {
        loadingSubject
            .debounce(20, TimeUnit.MILLISECONDS)
            .map { isLoading }
    }

    /**
     * Indicates whether repository is loading something now.
     */
    var isLoading: Boolean = false
        protected set(value) {
            if (field != value) {
                field = value
                loadingSubject.onNext(value)
            }
            field = value
        }

    /**
     * Indicates whether data is in actual state.
     */
    var isFresh = false
        protected set

    /**
     * Indicates whether repository has no data because it was never updated.
     */
    var isNeverUpdated = true
        protected set

    /**
     * Instantly starts data update.
     */
    abstract fun update(): Completable

    /**
     * Starts data update only on subscription.
     */
    open fun updateDeferred(): Completable {
        return Completable.defer { update() }
    }

    /**
     * Marks data as not fresh
     * @see Repository.isFresh
     */
    open fun invalidate() {
        synchronized(this) {
            isFresh = false
        }
    }

    /**
     * Instantly starts data update if it's not fresh.
     * @see Repository.isFresh
     */
    open fun updateIfNotFresh(): Completable {
        return synchronized(this) {
            if (!isFresh) {
                update()
            } else {
                Completable.complete()
            }
        }
    }

    /**
     * Starts data update it it's not fresh only on subscription.
     * @see Repository.isFresh
     */
    open fun updateIfNotFreshDeferred(): Completable {
        return Completable.defer { updateIfNotFresh() }
    }

    /**
     * Instantly starts data it it was ever updated
     * i.e. if someone was ever interested in this repo's data.
     */
    open fun updateIfEverUpdated(): Completable {
        return if (!isNeverUpdated)
            update()
        else
            Completable.complete()
    }
}