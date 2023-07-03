package ua.com.radiokot.photoprism.features.webview.logic

import android.webkit.HttpAuthHandler
import android.webkit.WebView
import okhttp3.HttpUrl
import ua.com.radiokot.photoprism.extension.kLogger

/**
 * A handler that proceeds the request with the session root URL credentials,
 * if they are present and the request matches the host. Ignores otherwise.
 */
class SessionRootUrlWebViewHttpAuthRequestHandler(
    envRootUrl: HttpUrl,
) : WebViewHttpAuthRequestHandler {
    private val log = kLogger("SessionRootUrlWVHARHandler")

    private val envRootUrlUsername: String = envRootUrl.username
    private val envRootUrlPassword: String = envRootUrl.password
    private val envRootUrlHost: String = envRootUrl.host

    override fun handleHttpAuthRequest(view: WebView, host: String, handler: HttpAuthHandler) {
        if (envRootUrlUsername.isEmpty() && envRootUrlPassword.isEmpty()) {
            log.debug {
                "handleHttpAuthRequest(): root_url_has_no_auth"
            }

            ignoreRequest(handler)
            return
        }

        if (host != envRootUrlHost) {
            log.debug {
                "handleHttpAuthRequest(): request_doesnt_match_env:" +
                        "\nrequestHost=$host"
            }

            ignoreRequest(handler)
            return
        }

        proceedRequest(handler, envRootUrlUsername, envRootUrlPassword)
    }

    private fun ignoreRequest(handler: HttpAuthHandler) {
        handler.cancel()

        log.debug {
            "ignoreRequest(): request_ignored"
        }
    }

    private fun proceedRequest(
        handler: HttpAuthHandler,
        username: String,
        password: String,
    ) {
        handler.proceed(username, password)

        log.debug {
            "proceedRequest(): request_proceeded"
        }
    }
}
