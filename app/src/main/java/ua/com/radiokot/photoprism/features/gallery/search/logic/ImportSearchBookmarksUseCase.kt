package ua.com.radiokot.photoprism.features.gallery.search.logic

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.kotlin.toCompletable
import io.reactivex.rxjava3.schedulers.Schedulers
import ua.com.radiokot.photoprism.extension.checkNotNull
import ua.com.radiokot.photoprism.extension.toSingle
import ua.com.radiokot.photoprism.features.gallery.data.model.SearchBookmark
import ua.com.radiokot.photoprism.features.gallery.data.storage.SearchBookmarksRepository
import java.io.File

/**
 * Imports the bookmarks from [fileUri]
 * into the [searchBookmarksRepository] using [backupStrategy].
 *
 * @see SearchBookmarksRepository.import
 */
class ImportSearchBookmarksUseCase(
    private val fileUri: Uri,
    private val backupStrategy: SearchBookmarksBackup,
    private val searchBookmarksRepository: SearchBookmarksRepository,
    private val contentResolver: ContentResolver,
) {
    private lateinit var readBookmarks: List<SearchBookmark>

    operator fun invoke(): Single<List<SearchBookmark>> {
        return checkFile()
            .flatMap { readBackup() }
            .doOnSuccess { readBookmarks = it }
            .flatMap { importReadBookmarks() }
            .map { searchBookmarksRepository.itemsList }
    }

    private fun checkFile(): Single<Boolean> = {
        contentResolver.query(
            fileUri,
            arrayOf(OpenableColumns.DISPLAY_NAME),
            null,
            null,
            null,
            null
        )
            .use { cursor ->
                checkNotNull(cursor) {
                    "File info response is required"
                }

                cursor.moveToFirst()

                val fileName = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    .takeIf { it >= 0 }
                    .checkNotNull { "Display name is required in the file info response" }
                )
                val fileExtension = "." + File(fileName).extension

                check(fileExtension == backupStrategy.fileExtension) {
                    "To use $backupStrategy, the file extension must be ${backupStrategy.fileExtension}," +
                            " while actual is $fileExtension"
                }
            }
    }.toCompletable().subscribeOn(Schedulers.io()).toSingleDefault(true)

    private fun readBackup(): Single<List<SearchBookmark>> = {
        contentResolver.openInputStream(fileUri).use { inputStream ->
            checkNotNull(inputStream) {
                "The input stream must be opened"
            }

            backupStrategy.readBackup(
                input = inputStream,
            )
        }
    }.toSingle().subscribeOn(Schedulers.io())

    private fun importReadBookmarks(): Single<Boolean> =
        searchBookmarksRepository
            .import(readBookmarks)
            .toSingleDefault(true)
}
