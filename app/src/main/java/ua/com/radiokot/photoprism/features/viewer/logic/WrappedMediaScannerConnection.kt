package ua.com.radiokot.photoprism.features.viewer.logic

fun interface WrappedMediaScannerConnection {
    fun scanFile(path: String, mimeType: String)
}