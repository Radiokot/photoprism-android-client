package ua.com.radiokot.photoprism.extension

val Throwable.shortSummary: String
    get() = this.message
        ?: javaClass.simpleName