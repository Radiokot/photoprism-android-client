package ua.com.radiokot.photoprism.extension

val Throwable.shortSummary: String
    get() {
        val simpleName = javaClass.simpleName
        val message = message
        return if (message != null)
            "$simpleName: $message"
        else
            simpleName
    }