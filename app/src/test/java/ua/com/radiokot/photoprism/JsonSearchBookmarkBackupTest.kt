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
                )
            ),
            SearchBookmark(
                id = 2,
                position = Math.PI,
                name = "Интересные фото 🍕",
                searchConfig = SearchConfig(
                    mediaTypes = setOf(),
                    before = null,
                    userQuery = "",
                    includePrivate = false,
                )
            ),
        )

        val outputStream = ByteArrayOutputStream()
        backup.writeBackup(bookmarks, outputStream)
        val outputJson = String(outputStream.toByteArray(), Charsets.UTF_8)

        Assert.assertEquals(
            """{"v":1,"d":{"b":[{"id":1,"pos":1.0,"n":"My camera","q":"quality:3 oleg&cam","mt":["RAW","VIDEO"],"priv":true},{"id":2,"pos":3.141592653589793,"n":"Интересные фото \uD83C\uDF55","q":"","mt":[],"priv":false}]}}""",
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
                )
            ),
            SearchBookmark(
                id = 2,
                position = Math.PI,
                name = "Интересные фото 🍕",
                searchConfig = SearchConfig(
                    mediaTypes = setOf(),
                    before = null,
                    userQuery = "",
                    includePrivate = false,
                )
            ),
        )

        val inputJson =
            """{"v":1,"d":{"b":[{"id":1,"pos":1.0,"n":"My camera","q":"quality:3 oleg&cam","mt":["RAW","VIDEO"],"priv":true},{"id":2,"pos":3.141592653589793,"n":"Интересные фото \uD83C\uDF55","q":"","mt":[],"priv":false}]}}"""
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