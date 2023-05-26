package ua.com.radiokot.photoprism

import com.fasterxml.jackson.module.kotlin.jsonMapper
import okio.IOException
import org.junit.Assert
import org.junit.BeforeClass
import org.junit.Test
import org.koin.core.component.KoinComponent
import org.koin.core.context.startKoin
import ua.com.radiokot.photoprism.features.gallery.data.model.GalleryMedia
import ua.com.radiokot.photoprism.features.gallery.data.model.SearchBookmark
import ua.com.radiokot.photoprism.features.gallery.data.model.SearchConfig
import ua.com.radiokot.photoprism.features.gallery.di.galleryFeatureModules
import ua.com.radiokot.photoprism.features.gallery.logic.JsonSearchBookmarksBackup
import ua.com.radiokot.photoprism.features.gallery.logic.UnsupportedVersionException
import java.io.ByteArrayOutputStream

class JsonSearchBookmarkBackupTest : KoinComponent {
    companion object {
        @BeforeClass
        @JvmStatic
        fun initKoin() {
            startKoin {
                modules(galleryFeatureModules)
            }
        }
    }

    @Test
    fun fileExtension() {
        val backup = JsonSearchBookmarksBackup(jsonMapper())
        Assert.assertEquals(".json", backup.fileExtension)
    }

    @Test
    fun export() {
        val backup = getKoin().get<JsonSearchBookmarksBackup>()

        val bookmarks = listOf(
            SearchBookmark(
                id = 1,
                position = 1.0,
                name = "My camera",
                searchConfig = SearchConfig(
                    mediaTypes = setOf(
                        GalleryMedia.TypeName.RAW,
                        GalleryMedia.TypeName.VIDEO,
                    ),
                    before = null,
                    userQuery = "quality:3 oleg&cam",
                    includePrivate = true,
                    albumUid = "ars1juz1456bluxz"
                )
            ),
            SearchBookmark(
                id = 2,
                position = Math.PI,
                name = "Интересные фото 🍕",
                searchConfig = SearchConfig(
                    mediaTypes = null,
                    before = null,
                    userQuery = "",
                    includePrivate = false,
                    albumUid = null,
                )
            ),
            SearchBookmark(
                id = 3,
                position = 25.5,
                name = "media types empty",
                searchConfig = SearchConfig(
                    mediaTypes = emptySet(),
                    before = null,
                    userQuery = "",
                    includePrivate = false,
                    albumUid = null,
                )
            ),
        )

        val outputStream = ByteArrayOutputStream()
        backup.writeBackup(bookmarks, outputStream)
        val outputJson = String(outputStream.toByteArray(), Charsets.UTF_8)

        Assert.assertEquals(
            """{"v":2,"d":{"b":[{"id":1,"p":1.0,"n":"My camera","q":"quality:3 oleg&cam","mt":["RAW","VIDEO"],"ip":true,"a":"ars1juz1456bluxz"},{"id":2,"p":3.141592653589793,"n":"Интересные фото \uD83C\uDF55","q":"","mt":null,"ip":false,"a":null},{"id":3,"p":25.5,"n":"media types empty","q":"","mt":[],"ip":false,"a":null}]}}""",
            outputJson,
        )
    }

    @Test
    fun import() {
        val backup = getKoin().get<JsonSearchBookmarksBackup>()

        val bookmarks = listOf(
            SearchBookmark(
                id = 1,
                position = 1.0,
                name = "My camera",
                searchConfig = SearchConfig(
                    mediaTypes = setOf(
                        GalleryMedia.TypeName.RAW,
                        GalleryMedia.TypeName.VIDEO,
                    ),
                    before = null,
                    userQuery = "quality:3 oleg&cam",
                    includePrivate = true,
                    albumUid = "ars1juz1456bluxz"
                )
            ),
            SearchBookmark(
                id = 2,
                position = Math.PI,
                name = "Интересные фото 🍕",
                searchConfig = SearchConfig(
                    mediaTypes = null,
                    before = null,
                    userQuery = "",
                    includePrivate = false,
                    albumUid = null,
                )
            ),
            SearchBookmark(
                id = 3,
                position = 25.5,
                name = "media types empty",
                searchConfig = SearchConfig(
                    mediaTypes = emptySet(),
                    before = null,
                    userQuery = "",
                    includePrivate = false,
                    albumUid = null,
                )
            ),
        )

        val inputJson =
            """{"v":2,"d":{"b":[{"id":1,"p":1.0,"n":"My camera","q":"quality:3 oleg&cam","mt":["RAW","VIDEO"],"ip":true,"a":"ars1juz1456bluxz"},{"id":2,"p":3.141592653589793,"n":"Интересные фото \uD83C\uDF55","q":"","mt":null,"ip":false,"a":null},{"id":3,"p":25.5,"n":"media types empty","q":"","mt":[],"ip":false,"a":null}]}}"""
        val inputStream = inputJson.byteInputStream(Charsets.UTF_8)
        val imported = backup.readBackup(inputStream)

        Assert.assertEquals(
            bookmarks.size,
            imported.size,
        )

        bookmarks.forEachIndexed { i, bookmark ->
            val importedBookmark = imported[i]

            Assert.assertEquals(bookmark, importedBookmark)

            Assert.assertEquals(bookmark.position, importedBookmark.position, 0.0)
            Assert.assertEquals(bookmark.name, importedBookmark.name)

            // As long as the searchConfig is a data class,
            // equals can be used to check all the fields.
            Assert.assertEquals(bookmark.searchConfig, importedBookmark.searchConfig)
            println(bookmark.searchConfig.copy()) // Test for data class – .copy exists.
        }
    }

    @Test
    fun importV1() {
        val backup = getKoin().get<JsonSearchBookmarksBackup>()

        val bookmarks = listOf(
            SearchBookmark(
                id = 1,
                position = 1.0,
                name = "My camera",
                searchConfig = SearchConfig(
                    mediaTypes = setOf(
                        GalleryMedia.TypeName.RAW,
                        GalleryMedia.TypeName.VIDEO,
                    ),
                    before = null,
                    userQuery = "quality:3 oleg&cam",
                    includePrivate = true,
                    albumUid = "ars1juz1456bluxz",
                )
            ),
            SearchBookmark(
                id = 2,
                position = Math.PI,
                name = "Интересные фото 🍕",
                searchConfig = SearchConfig(
                    mediaTypes = null,
                    before = null,
                    userQuery = "",
                    includePrivate = false,
                    albumUid = null,
                )
            ),
        )

        val inputJson =
            """{"v":1,"d":{"b":[{"id":1,"pos":1.0,"n":"My camera","q":"quality:3 oleg&cam","mt":["RAW","VIDEO"],"priv":true,"a":"ars1juz1456bluxz"},{"id":2,"pos":3.141592653589793,"n":"Интересные фото \uD83C\uDF55","q":"","mt":[],"priv":false,"a":null}]}}"""
        val inputStream = inputJson.byteInputStream(Charsets.UTF_8)
        val imported = backup.readBackup(inputStream)

        Assert.assertEquals(
            bookmarks.size,
            imported.size,
        )

        bookmarks.forEachIndexed { i, bookmark ->
            val importedBookmark = imported[i]

            Assert.assertEquals(bookmark, importedBookmark)

            Assert.assertEquals(bookmark.position, importedBookmark.position, 0.0)
            Assert.assertEquals(bookmark.name, importedBookmark.name)

            // As long as the searchConfig is a data class,
            // equals can be used to check all the fields.
            Assert.assertEquals(bookmark.searchConfig, importedBookmark.searchConfig)
            println(bookmark.searchConfig.copy()) // Test for data class – .copy exists.
        }
    }

    @Test(expected = UnsupportedVersionException::class)
    fun unsupportedVersion() {
        val backup = getKoin().get<JsonSearchBookmarksBackup>()

        val inputJson = """{"v":-1,"d":{}}"""
        val inputStream = inputJson.byteInputStream(Charsets.UTF_8)
        backup.readBackup(inputStream)
    }

    @Test(expected = IOException::class)
    fun malformed() {
        val backup = getKoin().get<JsonSearchBookmarksBackup>()

        val inputJson = """wtf"""
        val inputStream = inputJson.byteInputStream(Charsets.UTF_8)
        backup.readBackup(inputStream)
    }
}
