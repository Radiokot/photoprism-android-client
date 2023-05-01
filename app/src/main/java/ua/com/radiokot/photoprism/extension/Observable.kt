package ua.com.radiokot.photoprism.extension

import io.reactivex.rxjava3.core.Observable
import kotlin.reflect.cast

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