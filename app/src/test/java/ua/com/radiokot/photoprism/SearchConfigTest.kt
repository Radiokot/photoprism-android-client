package ua.com.radiokot.photoprism

import org.junit.Assert
import org.junit.Test
import ua.com.radiokot.photoprism.features.gallery.data.model.GalleryMedia
import ua.com.radiokot.photoprism.features.gallery.data.model.SearchConfig
import ua.com.radiokot.photoprism.util.LocalDate

class SearchConfigTest {
    @Test
    fun shouldBuildQuery_IfAllParamsSet() {
        SearchConfig(
            mediaTypes = setOf(
                GalleryMedia.TypeName.ANIMATED,
                GalleryMedia.TypeName.LIVE,
            ),
            albumUid = "a222",
            personIds = setOf(
                "s222222222222222", // subject ID
                "s333333333333333",
                "p2222222222222222222222222222222", // person UID
                "p3333333333333333333333333333333",
            ),
            beforeLocal = LocalDate(1699534800000),
            afterLocal = LocalDate(1699594800000),
            userQuery = "user query 123&321",
            includePrivate = true,
            onlyFavorite = true,
        ).apply {
            Assert.assertEquals(
                """
                    user query 123&321 type:animated|live before:"2023-11-10T13:00:00Z" after:"2023-11-09T05:40:00Z" public:false favorite:true album:a222 subject:s222222222222222&s333333333333333 face:p2222222222222222222222222222222&p3333333333333333333333333333333
                """.trimIndent(),
                getPhotoPrismQuery()
            )
        }
    }

    @Test
    fun shouldBuildMinimalQuery_IfConfigIsDefault() {
        Assert.assertEquals(
            " public:true",
            SearchConfig.DEFAULT.getPhotoPrismQuery()
        )
    }
}
