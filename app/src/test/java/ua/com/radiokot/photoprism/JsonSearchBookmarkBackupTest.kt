package ua.com.radiokot.photoprism

import com.fasterxml.jackson.module.kotlin.jsonMapper
import okio.IOException
import org.junit.AfterClass
import org.junit.Assert
import org.junit.BeforeClass
import org.junit.Test
import org.koin.core.component.KoinComponent
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import ua.com.radiokot.photoprism.features.gallery.data.model.GalleryMedia
import ua.com.radiokot.photoprism.features.gallery.data.model.SearchBookmark
import ua.com.radiokot.photoprism.features.gallery.data.model.SearchConfig
import ua.com.radiokot.photoprism.features.gallery.galleryFeatureModule
import ua.com.radiokot.photoprism.features.gallery.search.logic.JsonSearchBookmarksBackup
import ua.com.radiokot.photoprism.features.gallery.search.logic.UnsupportedVersionException
import java.io.ByteArrayOutputStream

class JsonSearchBookmarkBackupTest : KoinComponent {
    companion object {
        @BeforeClass
        @JvmStatic
        fun initKoin() {
            startKoin {
                modules(galleryFeatureModule)
            }
        }

        @AfterClass
        @JvmStatic
        fun tearDownKoin() {
            stopKoin()
        }

        val BOOKMARKS = listOf(
            SearchBookmark(
                id = 1,
                position = 1.0,
                name = "My camera",
                searchConfig = SearchConfig(
                    mediaTypes = setOf(
                        GalleryMedia.TypeName.RAW,
                        GalleryMedia.TypeName.VIDEO,
                    ),
                    beforeLocal = null,
                    afterLocal = null,
                    userQuery = "quality:3 oleg&cam",
                    includePrivate = true,
                    onlyFavorite = false,
                    albumUid = "ars1juz1456bluxz",
                    personIds = emptySet(),
                )
            ),
            SearchBookmark(
                id = 2,
                position = Math.PI,
                name = "Интересные фото 🍕",
                searchConfig = SearchConfig(
                    mediaTypes = null,
                    beforeLocal = null,
                    afterLocal = null,
                    userQuery = "",
                    includePrivate = false,
                    onlyFavorite = false,
                    albumUid = null,
                    personIds = setOf("p2132523232"),
                )
            ),
            SearchBookmark(
                id = 3,
                position = 25.5,
                name = "media types empty",
                searchConfig = SearchConfig(
                    mediaTypes = emptySet(),
                    beforeLocal = null,
                    afterLocal = null,
                    userQuery = "",
                    includePrivate = false,
                    onlyFavorite = true,
                    albumUid = null,
                    personIds = setOf("p2132523232", "pwr9hh8ef9w3"),
                    personFilterOperator = SearchConfig.PersonFilterOperator.ANY,
                )
            ),
        )
    }

    @Test
    fun fileExtension() {
        val backup = JsonSearchBookmarksBackup(jsonMapper())
        Assert.assertEquals(".json", backup.fileExtension)
    }

    @Test
    fun export() {
        val backup = getKoin().get<JsonSearchBookmarksBackup>()

        val outputStream = ByteArrayOutputStream()
        backup.writeBackup(BOOKMARKS, outputStream)
        val outputJson = String(outputStream.toByteArray(), Charsets.UTF_8)

        Assert.assertEquals(
            """{"v":4,"d":{"b":[{"id":1,"p":1.0,"n":"My camera","q":"quality:3 oleg&cam","mt":["RAW","VIDEO"],"ip":true,"of":false,"a":"ars1juz1456bluxz","pe":[],"po":"ALL"},{"id":2,"p":3.141592653589793,"n":"Интересные фото \uD83C\uDF55","q":"","mt":null,"ip":false,"of":false,"a":null,"pe":["p2132523232"],"po":"ALL"},{"id":3,"p":25.5,"n":"media types empty","q":"","mt":[],"ip":false,"of":true,"a":null,"pe":["p2132523232","pwr9hh8ef9w3"],"po":"ANY"}]}}""",
            outputJson,
        )
    }

    @Test
    fun import() {
        val backup = getKoin().get<JsonSearchBookmarksBackup>()

        val inputJson =
            """{"v":4,"d":{"b":[{"id":1,"p":1.0,"n":"My camera","q":"quality:3 oleg&cam","mt":["RAW","VIDEO"],"ip":true,"of":false,"a":"ars1juz1456bluxz","pe":[],"po":"ALL"},{"id":2,"p":3.141592653589793,"n":"Интересные фото \uD83C\uDF55","q":"","mt":null,"ip":false,"of":false,"a":null,"pe":["p2132523232"],"po":"ALL"},{"id":3,"p":25.5,"n":"media types empty","q":"","mt":[],"ip":false,"of":true,"a":null,"pe":["p2132523232","pwr9hh8ef9w3"],"po":"ANY"}]}}"""
        val inputStream = inputJson.byteInputStream(Charsets.UTF_8)
        val imported = backup.readBackup(inputStream)

        Assert.assertEquals(
            BOOKMARKS.size,
            imported.size,
        )

        BOOKMARKS.forEachIndexed { i, bookmark ->
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
    fun importV3() {
        val backup = getKoin().get<JsonSearchBookmarksBackup>()

        val inputJson =
            """{"v":3,"d":{"b":[{"id":1,"p":1.0,"n":"My camera","q":"quality:3 oleg&cam","mt":["RAW","VIDEO"],"ip":true,"of":false,"a":"ars1juz1456bluxz","pe":[]},{"id":2,"p":3.141592653589793,"n":"Интересные фото \uD83C\uDF55","q":"","mt":null,"ip":false,"of":false,"a":null,"pe":["p2132523232"]},{"id":3,"p":25.5,"n":"media types empty","q":"","mt":[],"ip":false,"of":true,"a":null,"pe":["p2132523232","pwr9hh8ef9w3"]}]}}"""
        val inputStream = inputJson.byteInputStream(Charsets.UTF_8)
        val imported = backup.readBackup(inputStream)

        Assert.assertEquals(
            BOOKMARKS.size,
            imported.size,
        )

        BOOKMARKS.forEachIndexed { i, bookmark ->
            val importedBookmark = imported[i]
            val expectedBookmark = bookmark.copy(
                searchConfig = bookmark.searchConfig.copy(
                    personFilterOperator = SearchConfig.PersonFilterOperator.ALL,
                )
            )

            Assert.assertEquals(expectedBookmark, importedBookmark)

            Assert.assertEquals(expectedBookmark.position, importedBookmark.position, 0.0)
            Assert.assertEquals(expectedBookmark.name, importedBookmark.name)

            // As long as the searchConfig is a data class,
            // equals can be used to check all the fields.
            Assert.assertEquals(expectedBookmark.searchConfig, importedBookmark.searchConfig)
            println(expectedBookmark.searchConfig.copy()) // Test for data class – .copy exists.
        }
    }

    @Test
    fun importV2() {
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
                    beforeLocal = null,
                    afterLocal = null,
                    userQuery = "quality:3 oleg&cam",
                    includePrivate = true,
                    onlyFavorite = false,
                    albumUid = "ars1juz1456bluxz",
                    personIds = emptySet(),
                )
            )
        )

        val inputJson =
            """{"v":2,"d":{"b":[{"id":1,"p":1.0,"n":"My camera","q":"quality:3 oleg&cam","mt":["RAW","VIDEO"],"ip":true,"a":"ars1juz1456bluxz"}]}}"""
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
                    beforeLocal = null,
                    afterLocal = null,
                    userQuery = "quality:3 oleg&cam",
                    includePrivate = true,
                    onlyFavorite = false,
                    albumUid = "ars1juz1456bluxz",
                    personIds = emptySet(),
                )
            ),
            SearchBookmark(
                id = 2,
                position = Math.PI,
                name = "Интересные фото 🍕",
                searchConfig = SearchConfig(
                    mediaTypes = null,
                    beforeLocal = null,
                    afterLocal = null,
                    userQuery = "",
                    includePrivate = false,
                    onlyFavorite = false,
                    albumUid = null,
                    personIds = emptySet(),
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
