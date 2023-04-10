package ua.com.radiokot.photoprism.features.gallery.logic

import ua.com.radiokot.photoprism.features.gallery.data.model.SearchBookmark
import java.io.InputStream
import java.io.OutputStream

interface SearchBookmarksBackup {
    val fileExtension: String

    fun exportBookmarks(
        bookmarks: List<SearchBookmark>,
        output: OutputStream
    )

    fun importBookmarks(input: InputStream): List<SearchBookmark>
}