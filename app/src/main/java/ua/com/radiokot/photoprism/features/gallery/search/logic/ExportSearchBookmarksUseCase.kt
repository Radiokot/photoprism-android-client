package ua.com.radiokot.photoprism.features.gallery.search.logic

import android.content.Intent
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.kotlin.toCompletable
import io.reactivex.rxjava3.schedulers.Schedulers
import ua.com.radiokot.photoprism.extension.toSingle
import ua.com.radiokot.photoprism.features.gallery.data.model.SearchBookmark
import ua.com.radiokot.photoprism.features.gallery.data.storage.SearchBookmarksRepository
import ua.com.radiokot.photoprism.features.gallery.logic.FileReturnIntentCreator
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Exports the bookmarks from [searchBookmarksRepository]
 * into a file determined by [backupStrategy],
 * returning the file sharing intent.
 */
class ExportSearchBookmarksUseCase(
    private val exportDir: File,
    private val backupStrategy: SearchBookmarksBackup,
    private val fileReturnIntentCreator: FileReturnIntentCreator,
    private val searchBookmarksRepository: SearchBookmarksRepository,
) {
    private val sharingFileDisplayName =
        "photoprism_bookmarks_${OUTPUT_FILE_DATE_SUFFIX_FORMAT.format(Date())}" +
                backupStrategy.fileExtension

    private lateinit var bookmarks: List<SearchBookmark>
    private lateinit var outputFile: File

    operator fun invoke(): Single<Intent> {
        return collectBookmarks()
            .doOnSuccess { bookmarks = it }
            .flatMap { getOutputFile() }
            .doOnSuccess { outputFile = it }
            .flatMap { writeBackup() }
            .flatMap { getSharingIntent() }
    }

    private fun collectBookmarks(): Single<List<SearchBookmark>> =
        searchBookmarksRepository
            .updateIfNotFreshDeferred()
            .toSingle { searchBookmarksRepository.itemsList }

    private fun getOutputFile(): Single<File> = {
        File(
            exportDir.also(File::mkdirs),
            "bookmarks_backup",
        )
    }.toSingle().subscribeOn(Schedulers.io())

    private fun writeBackup(): Single<Boolean> = {
        outputFile.outputStream().use { outputStream ->
            backupStrategy.writeBackup(
                bookmarks = bookmarks,
                output = outputStream
            )
        }
    }.toCompletable().toSingleDefault(true).subscribeOn(Schedulers.io())

    private fun getSharingIntent(): Single<Intent> = {
        fileReturnIntentCreator.createIntent(
            fileToReturn = outputFile,
            mimeType = backupStrategy.fileMimeType,
            displayName = sharingFileDisplayName,
        )
    }.toSingle()

    private companion object {
        private val OUTPUT_FILE_DATE_SUFFIX_FORMAT =
            SimpleDateFormat("yyyy-MM-dd_HH.mm.ss", Locale.ENGLISH)
    }
}
