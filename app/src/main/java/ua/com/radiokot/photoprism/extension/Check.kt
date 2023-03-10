package ua.com.radiokot.photoprism.extension

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

@OptIn(ExperimentalContracts::class)
fun <T : Any> T?.checkNotNull(): T {
    contract {
        returns() implies (this@checkNotNull != null)
    }

    return checkNotNull(this)
}

@OptIn(ExperimentalContracts::class)
inline fun <T : Any> T?.checkNotNull(lazyMessage: () -> Any): T {
    contract {
        returns() implies (this@checkNotNull != null)
    }

    return checkNotNull(this, lazyMessage)
}