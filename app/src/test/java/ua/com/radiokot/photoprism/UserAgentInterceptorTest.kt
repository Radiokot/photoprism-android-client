package ua.com.radiokot.photoprism

import okhttp3.Interceptor
import org.junit.Assert
import org.junit.Test
import ua.com.radiokot.photoprism.api.util.UserAgentInterceptor

class UserAgentInterceptorTest {
    @Test
    fun simpleUserAgentString() {
        val interceptor = UserAgentInterceptor(
            name = "PPA",
            version = "1.2.3",
            extension = null,
        )

        Assert.assertEquals("PPA/1.2.3", interceptor.userAgent)
    }

    @Test
    fun userAgentStringWithExtension() {
        val interceptor = UserAgentInterceptor(
            name = "PPA",
            version = "1.2.3",
            extension = "okhttp/7.4",
        )

        Assert.assertEquals("PPA/1.2.3 okhttp/7.4", interceptor.userAgent)
    }
}
