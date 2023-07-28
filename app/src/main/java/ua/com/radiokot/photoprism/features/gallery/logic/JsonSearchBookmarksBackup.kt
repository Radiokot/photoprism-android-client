package ua.com.radiokot.photoprism.features.gallery.logic

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.JsonNode
import okio.IOException
import ua.com.radiokot.photoprism.di.JsonObjectMapper
import ua.com.radiokot.photoprism.features.gallery.data.model.GalleryMedia
import ua.com.radiokot.photoprism.features.gallery.data.model.SearchBookmark
import ua.com.radiokot.photoprism.features.gallery.data.model.SearchConfig
import java.io.InputStream
import java.io.OutputStream

class UnsupportedVersionException(version: Int?) :
    IOException("Unsupported backup version $version")

class JsonSearchBookmarksBackup(
    private val jsonObjectMapper: JsonObjectMapper,
) : SearchBookmarksBackup {
    override val fileExtension: String = ".json"
    override val fileMimeType: String = "application/json"

    override fun writeBackup(bookmarks: List<SearchBookmark>, output: OutputStream) {
        // Trying to solve F-Droid build bytecode magic.
        val data: JsonNode = this.jsonObjectMapper.valueToTree(createBackupData(bookmarks))
        this.jsonObjectMapper.writeValue(
            output,
            BookmarksBackup(
                version = VERSION,
                data = data,
            )
        )
    }

    override fun readBackup(input: InputStream): List<SearchBookmark> {
        val backup: BookmarksBackup =
            jsonObjectMapper.readValue(input, BookmarksBackup::class.java)

        return when (val version = backup.version) {
            VERSION ->
                readBackupData(backup.data).bookmarks.map(BackupData.Bookmark::toSource)

            2 ->
                readBackupDataV2(backup.data).bookmarks.map(BackupDataV2.Bookmark::toSource)

            1 ->
                readBackupDataV1(backup.data).bookmarks.map(BackupDataV1.Bookmark::toSource)

            else ->
                throw UnsupportedVersionException(version)
        }
    }

    private fun createBackupData(bookmarks: List<SearchBookmark>) = BackupData(
        bookmarks = bookmarks.map { BackupData.Bookmark(it) }
    )

    private fun readBackupData(dataTree: JsonNode): BackupData =
        jsonObjectMapper.treeToValue(dataTree, BackupData::class.java)

    private fun readBackupDataV2(dataTree: JsonNode): BackupDataV2 =
        jsonObjectMapper.treeToValue(dataTree, BackupDataV2::class.java)

    private fun readBackupDataV1(dataTree: JsonNode): BackupDataV1 =
        jsonObjectMapper.treeToValue(dataTree, BackupDataV1::class.java)

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
            @JsonProperty("p")
            val position: Double,
            @JsonProperty("n")
            val name: String,
            @JsonProperty("q")
            val userQuery: String,
            @JsonProperty("mt")
            val mediaTypes: List<String>?,
            @JsonProperty("ip")
            val includePrivate: Boolean,
            @JsonProperty("a")
            val albumUid: String?,
            @JsonProperty("pe")
            val personIds: Set<String>,
        ) {
            constructor(source: SearchBookmark) : this(
                id = source.id,
                position = source.position,
                name = source.name,
                userQuery = source.searchConfig.userQuery,
                mediaTypes = source.searchConfig.mediaTypes
                    ?.map(GalleryMedia.TypeName::toString),
                includePrivate = source.searchConfig.includePrivate,
                albumUid = source.searchConfig.albumUid,
                personIds = source.searchConfig.personIds,
            )

            fun toSource() = SearchBookmark(
                id = id,
                name = name,
                position = position,
                searchConfig = SearchConfig(
                    userQuery = userQuery,
                    mediaTypes = mediaTypes
                        ?.map { GalleryMedia.TypeName.valueOf(it) }
                        ?.toSet(),
                    includePrivate = includePrivate,
                    beforeLocal = null,
                    albumUid = albumUid,
                    personIds = personIds,
                )
            )
        }
    }

    class BackupDataV2
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
            @JsonProperty("p")
            val position: Double,
            @JsonProperty("n")
            val name: String,
            @JsonProperty("q")
            val userQuery: String,
            @JsonProperty("mt")
            val mediaTypes: List<String>?,
            @JsonProperty("ip")
            val includePrivate: Boolean,
            @JsonProperty("a")
            val albumUid: String?,
        ) {
            fun toSource() = SearchBookmark(
                id = id,
                name = name,
                position = position,
                searchConfig = SearchConfig(
                    userQuery = userQuery,
                    mediaTypes = mediaTypes
                        ?.map { GalleryMedia.TypeName.valueOf(it) }
                        ?.toSet(),
                    includePrivate = includePrivate,
                    beforeLocal = null,
                    albumUid = albumUid,
                    personIds = emptySet()
                )
            )
        }
    }

    class BackupDataV1
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
            @JsonProperty("a")
            val albumUid: String?,
        ) {
            fun toSource() = SearchBookmark(
                id = id,
                name = name,
                position = position,
                searchConfig = SearchConfig(
                    userQuery = userQuery,
                    mediaTypes = mediaTypes
                        .takeUnless(List<*>::isEmpty)
                        ?.map { GalleryMedia.TypeName.valueOf(it) }
                        ?.toSet(),
                    includePrivate = includePrivate,
                    beforeLocal = null,
                    albumUid = albumUid,
                    personIds = emptySet(),
                )
            )
        }
    }

    private companion object {
        private const val VERSION = 3
    }
}
