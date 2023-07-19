package ua.com.radiokot.photoprism

import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert
import org.junit.Test
import ua.com.radiokot.photoprism.features.gallery.logic.PhotoPrismMediaWebUrlFactory

class PhotoPrismMediaWebUrlFactoryTest {
    @Test
    fun getWebViewUrl() {
        val factory = PhotoPrismMediaWebUrlFactory(
            webLibraryUrl = "https://photoprism.me/library/".toHttpUrl(),
        )
        val url = factory.getWebViewUrl(
            uid = "prtkeob2g20yq13w",
        )
        Assert.assertEquals(
            "https://photoprism.me/library/browse?q=uid%3Aprtkeob2g20yq13w%20quality%3A0%20public%3Afalse&public=false&view=cards",
            url
        )
    }
}
