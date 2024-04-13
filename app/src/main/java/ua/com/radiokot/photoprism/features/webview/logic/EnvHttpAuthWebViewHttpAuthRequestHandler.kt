package ua.com.radiokot.photoprism.features.webview.logic

import android.webkit.HttpAuthHandler
import android.webkit.WebView
import okhttp3.HttpUrl
import okio.ByteString.Companion.decodeBase64
import ua.com.radiokot.photoprism.extension.kLogger

/**
 * A handler that proceeds the request with the env HTTP auth,
 * if it is present and the request matches the host. Ignores otherwise.
 */
class EnvHttpAuthWebViewHttpAuthRequestHandler(
    envRootUrl: HttpUrl,
    envHttpAuth: String?,
) : WebViewHttpAuthRequestHandler {
    private val log = kLogger("SessionHttpAuthWVHARHandler")

    private val envRootUrlHost: String = envRootUrl.host
    private val envHttpAuthCredentials: Pair<String, String>? =
        envHttpAuth
            ?.substringAfter("Basic ", "")
            ?.takeIf(String::isNotEmpty)
            ?.decodeBase64()
            ?.string(Charsets.ISO_8859_1)
            ?.split(':')
            ?.takeIf { it.size == 2 }
            ?.let { it[0] to it[1] }

    override fun handleHttpAuthRequest(view: WebView, host: String, handler: HttpAuthHandler) {
        if (envHttpAuthCredentials == null) {
            log.debug {
                "handleHttpAuthRequest(): no_http_auth_credentials_set"
            }

            ignoreRequest(handler)
            return
        }

        if (host != envRootUrlHost) {
            log.debug {
                "handleHttpAuthRequest(): request_host_mismatch:" +
                        "\nrequestHost=$host"
            }

            ignoreRequest(handler)
            return
        }

        handler.proceed(envHttpAuthCredentials.first, envHttpAuthCredentials.second)
        log.debug {
            "proceedRequest(): request_proceeded"
        }
    }

    private fun ignoreRequest(handler: HttpAuthHandler) {
        handler.cancel()

        log.debug {
            "ignoreRequest(): request_ignored"
        }
    }
}
