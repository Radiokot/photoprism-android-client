package ua.com.radiokot.photoprism.extension

/**
 * Returns a list containing only the successful (i.e. without exception)
 * results of applying the given [transform] function
 * to each element in the original collection.
 */
inline fun <T, R : Any> Iterable<T>.mapSuccessful(transform: (T) -> R): List<R> {
    return this.mapNotNull { tryOrNull { transform(it) } }
}

/**
 * Returns a sequence containing only the successful (i.e. without exception)
 * results of applying the given [transform] function
 * to each element in the original sequence.
 */
inline fun <T, R : Any> Sequence<T>.mapSuccessful(crossinline transform: (T) -> R): Sequence<R> {
    return this.mapNotNull { tryOrNull { transform(it) } }
}
