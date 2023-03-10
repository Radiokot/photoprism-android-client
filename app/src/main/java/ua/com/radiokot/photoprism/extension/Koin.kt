package ua.com.radiokot.photoprism.extension

import org.koin.core.Koin
import org.koin.core.scope.Scope

inline fun <reified T : Number> Koin.getNumericProperty(key: String): T? {
    val value: String = getProperty(key)
        ?: return null

    return when (T::class) {
        Byte::class ->
            value.toByteOrNull() as T?
        Short::class ->
            value.toShortOrNull() as T?
        Int::class ->
            value.toIntOrNull() as T?
        Long::class ->
            value.toLongOrNull() as T?
        Float::class ->
            value.toFloatOrNull() as T?
        Double::class ->
            value.toDoubleOrNull() as T?
        else ->
            value as? T?
    }
}

inline fun <reified T : Number> Koin.getNumericProperty(key: String, defaultValue: T): T {
    return getNumericProperty(key)
        ?: defaultValue
}

inline fun <reified T : Number> Scope.getNumericProperty(key: String): T? {
    val value: String = getPropertyOrNull(key)
        ?: return null

    return when (T::class) {
        Byte::class ->
            value.toByteOrNull() as T?
        Short::class ->
            value.toShortOrNull() as T?
        Int::class ->
            value.toIntOrNull() as T?
        Long::class ->
            value.toLongOrNull() as T?
        Float::class ->
            value.toFloatOrNull() as T?
        Double::class ->
            value.toDoubleOrNull() as T?
        else ->
            value as? T?
    }
}

inline fun <reified T : Number> Scope.getNumericProperty(key: String, defaultValue: T): T {
    return getNumericProperty(key)
        ?: defaultValue
}
