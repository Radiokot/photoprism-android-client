package ua.com.radiokot.photoprism

import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.Assert
import org.junit.Test
import ua.com.radiokot.photoprism.api.util.HeaderInterceptor

class UserAgentInterceptorTest {
    @Test
    fun simpleUserAgentString() {
        val interceptor = HeaderInterceptor.userAgent(
            name = "PPA",
            version = "1.2.3",
            extension = null,
        )

        Assert.assertEquals("User-Agent", interceptor.name)
        Assert.assertEquals("PPA/1.2.3", interceptor.lazyValue())
    }

    @Test
    fun userAgentStringWithExtension() {
        val interceptor = HeaderInterceptor.userAgent(
            name = "PPA",
            version = "1.2.3",
            extension = "okhttp/7.4",
        )

        Assert.assertEquals("User-Agent", interceptor.name)
        Assert.assertEquals("PPA/1.2.3 okhttp/7.4", interceptor.lazyValue())
    }
}
