package ua.com.radiokot.photoprism.extension

import io.reactivex.rxjava3.core.Single
import java.util.concurrent.Callable
import java.util.concurrent.Future

fun <T : Any> Callable<out T>.toSingle(): Single<T> = Single.fromCallable(this)
fun <T : Any> Future<out T>.toSingle(): Single<T> = Single.fromFuture(this)
fun <T : Any> (() -> T).toSingle(): Single<T> = Single.fromCallable(this)