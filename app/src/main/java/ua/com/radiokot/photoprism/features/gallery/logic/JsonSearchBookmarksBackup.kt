package ua.com.radiokot.photoprism.features.gallery.logic

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.JsonNode
import ua.com.radiokot.photoprism.di.JsonObjectMapper
import ua.com.radiokot.photoprism.features.gallery.data.model.GalleryMedia
import ua.com.radiokot.photoprism.features.gallery.data.model.SearchBookmark
import ua.com.radiokot.photoprism.features.gallery.data.model.SearchConfig
import java.io.InputStream
import java.io.OutputStream

class JsonSearchBookmarksBackup(
    private val jsonObjectMapper: JsonObjectMapper,
) : SearchBookmarksBackup {
    override val fileExtension: String = ".json"

    override fun exportBookmarks(bookmarks: List<SearchBookmark>, output: OutputStream) {
        jsonObjectMapper.writeValue(
            output,
            BookmarksBackup(
                version = VERSION,
                data = jsonObjectMapper.valueToTree(createBackupData(bookmarks))
            )
        )
    }

    override fun importBookmarks(input: InputStream): List<SearchBookmark> {
        val backup: BookmarksBackup =
            jsonObjectMapper.readValue(input, BookmarksBackup::class.java)

        val data = when (val version = backup.version) {
            1 -> readBackupData(backup.data)
            else -> error("Unsupported backup version $version")
        }

        return data.bookmarks.map(BackupData.Bookmark::toSource)
    }

    private fun createBackupData(bookmarks: List<SearchBookmark>) = BackupData(
        bookmarks = bookmarks.map { BackupData.Bookmark(it) }
    )

    private fun readBackupData(dataTree: JsonNode): BackupData =
        jsonObjectMapper.treeToValue(dataTree, BackupData::class.java)

    private class BookmarksBackup
    @JsonCreator
    constructor(
        @JsonProperty("v")
        val version: Int,
        @JsonProperty("d")
        val data: JsonNode,
    )

    class BackupData
    @JsonCreator
    constructor(
        @JsonProperty("b")
        val bookmarks: List<Bookmark>,
    ) {
        class Bookmark
        @JsonCreator
        constructor(
            @JsonProperty("id")
            val id: Long,
            @JsonProperty("pos")
            val position: Double,
            @JsonProperty("n")
            val name: String,
            @JsonProperty("q")
            val userQuery: String,
            @JsonProperty("mt")
            val mediaTypes: List<String>,
            @JsonProperty("priv")
            val includePrivate: Boolean,
        ) {
            constructor(source: SearchBookmark) : this(
                id = source.id,
                position = source.position,
                name = source.name,
                userQuery = source.searchConfig.userQuery,
                mediaTypes = source.searchConfig.mediaTypes
                    .map(GalleryMedia.TypeName::toString),
                includePrivate = source.searchConfig.includePrivate,
            )

            fun toSource() = SearchBookmark(
                id = id,
                name = name,
                position = position,
                searchConfig = SearchConfig(
                    userQuery = userQuery,
                    mediaTypes = mediaTypes
                        .map { GalleryMedia.TypeName.valueOf(it) }
                        .toSet(),
                    includePrivate = includePrivate,
                    before = null,
                )
            )
        }
    }

    private companion object {
        private const val VERSION = 1
    }
}