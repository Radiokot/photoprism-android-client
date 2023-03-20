package ua.com.radiokot.photoprism.features.viewer.logic

interface WrappedMediaScannerConnection {
    fun scanFile(path: String, mimeType: String)
}