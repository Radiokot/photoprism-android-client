package ua.com.radiokot.photoprism.extension

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.subjects.Subject
import org.koin.core.scope.Scope
import org.koin.core.scope.ScopeCallback
import java.util.concurrent.Callable
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.reflect.cast

fun <T : Any> Callable<out T>.toSingle(): Single<T> = Single.fromCallable(this)
fun <T : Any> Future<out T>.toSingle(): Single<T> = Single.fromFuture(this)
fun <T : Any> (() -> T).toSingle(): Single<T> = Single.fromCallable(this)

fun <T : Any> Callable<out T?>.toMaybe(): Maybe<T> = Maybe.fromCallable(this)
fun <T : Any> Future<out T?>.toMaybe(): Maybe<T> = Maybe.fromFuture(this)
fun <T : Any> (() -> T?).toMaybe(): Maybe<T> = Maybe.fromCallable(this)

/**
 * Filters items emitted by the current [Observable] by only emitting those that are
 * instances of the type [R].
 *
 * @see filter
 * @see map
 *
 * @return the new [Observable] instance
 */
inline fun <reified R : Any> Observable<*>.filterIsInstance(): Observable<R> =
    filter { R::class.isInstance(it) }
        .map { R::class.cast(it) }

/**
 * @return [Observable] that emits this subject's items on the Android main thread.
 */
fun <T : Any> Subject<T>.toMainThreadObservable(): Observable<T> =
    observeOn(AndroidSchedulers.mainThread())

private class LifecycleDisposable(obj: Disposable) :
    DefaultLifecycleObserver, Disposable by obj {
    override fun onDestroy(owner: LifecycleOwner) {
        if (!isDisposed) {
            dispose()
        }
    }
}

/**
 * Ensures that the disposable is disposed at [Lifecycle.Event.ON_DESTROY] event.
 */
fun <T : Disposable> T.autoDispose(lifecycleOwner: LifecycleOwner) = apply {
    lifecycleOwner.lifecycle.addObserver(LifecycleDisposable(this))
}

/**
 * Ensures that the disposable is disposed at [viewModel] clearing.
 */
fun <T : Disposable> T.autoDispose(viewModel: ViewModel) = apply {
    viewModel.addCloseable {
        if (!isDisposed) {
            dispose()
        }
    }
}

private class ScopeDisposable(obj: Disposable) :
    ScopeCallback, Disposable by obj {
    override fun onScopeClose(scope: Scope) {
        if (!isDisposed) {
            dispose()
        }
    }
}

/**
 * Ensures that the disposable is disposed at [scope] closing.
 */
fun <T : Disposable> T.autoDispose(scope: Scope) = apply {
    scope.registerCallback(ScopeDisposable(this))
}

/**
 * An [Observable.retry] with [times] which has a delay before resubscribing.
 *
 * @see [Observable.retry]
 */
fun <T : Any> Observable<T>.retryWithDelay(
    times: Int,
    delay: Long,
    unit: TimeUnit,
) = retryWhen { errors ->
    val retries = AtomicInteger(0)
    errors.flatMap { error ->
        if (retries.incrementAndGet() > times)
            Observable.error<Throwable>(error)
        else
            Observable.timer(delay, unit)
    }
}

/**
 * A [Single.retry] with [times] which has a delay before resubscribing.
 *
 * @see [Single.retry]
 */
fun <T : Any> Single<T>.retryWithDelay(
    times: Int,
    delay: Long,
    unit: TimeUnit,
) = retryWhen { errors ->
    val retries = AtomicInteger(0)
    errors.flatMap { error ->
        if (retries.incrementAndGet() > times)
            Flowable.error<Throwable>(error)
        else
            Flowable.timer(delay, unit)
    }
}

/**
 * A [Completable.retry] with [times] which has a delay before resubscribing.
 *
 * @see [Completable.retry]
 */
fun Completable.retryWithDelay(
    times: Int,
    delay: Long,
    unit: TimeUnit,
) = retryWhen { errors ->
    val retries = AtomicInteger(0)
    errors.flatMap { error ->
        if (retries.incrementAndGet() > times)
            Flowable.error<Throwable>(error)
        else
            Flowable.timer(delay, unit)
    }
}
