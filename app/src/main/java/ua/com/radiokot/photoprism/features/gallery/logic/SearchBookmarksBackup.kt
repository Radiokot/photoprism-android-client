package ua.com.radiokot.photoprism.features.gallery.logic

import ua.com.radiokot.photoprism.features.gallery.data.model.SearchBookmark
import java.io.InputStream
import java.io.OutputStream

/**
 * A file backup strategy for search bookmarks.
 */
interface SearchBookmarksBackup {
    /**
     * File extension with "."
     */
    val fileExtension: String

    val fileMimeType: String

    fun writeBackup(
        bookmarks: List<SearchBookmark>,
        output: OutputStream
    )

    fun readBackup(input: InputStream): List<SearchBookmark>
}