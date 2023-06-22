package ua.com.radiokot.photoprism.features.webview.logic

import android.content.Context
import android.os.Looper
import android.security.KeyChain
import android.webkit.ClientCertRequest
import android.webkit.WebView
import ua.com.radiokot.photoprism.env.data.model.EnvConnectionParams
import ua.com.radiokot.photoprism.extension.kLogger
import ua.com.radiokot.photoprism.extension.tryOrNull
import java.security.PrivateKey
import java.security.cert.X509Certificate

/**
 * A handler that proceeds the request with the session client certificate,
 * if it is present and matches the host. Ignores otherwise.
 *
 * **Must be called in a non-main thread because of [KeyChain] access**
 *
 * @param envConnectionParams environment of the session
 * @param context app context to access [KeyChain]
 */
class SessionCertWebViewClientCertRequestHandler(
    envConnectionParams: EnvConnectionParams,
    private val context: Context,
) : WebViewClientCertRequestHandler {
    private val log = kLogger("SessionCertWVCCRHandler")

    private val envHost: String = envConnectionParams.rootUrl.host
    private val envPort: Int = envConnectionParams.rootUrl.port
    private val envCertAlias: String? = envConnectionParams.clientCertificateAlias

    override fun handleClientCertRequest(view: WebView, request: ClientCertRequest) {
        check(Thread.currentThread() != Looper.getMainLooper().thread) {
            "The handler must be called in a non-main thread because of KeyChain access"
        }

        if (envCertAlias == null) {
            log.debug {
                "handleClientCertRequest(): env_connection_has_no_cert"
            }

            ignoreRequest(request)
            return
        }

        val requestHost: String = request.host
        val requestPort: Int = request.port

        if (requestHost != envHost || requestPort != envPort) {
            log.debug {
                "handleClientCertRequest(): request_doesnt_match_env:" +
                        "\nrequestHost=$requestHost," +
                        "\nrequestPort=$requestPort"
            }

            ignoreRequest(request)
            return
        }

        val privateKey = tryOrNull {
            KeyChain.getPrivateKey(context, envCertAlias)
        }
        if (privateKey == null) {
            log.debug {
                "handleClientCertRequest(): private_key_getting_failed:" +
                        "\nalias=$envCertAlias"
            }

            ignoreRequest(request)
            return
        }

        val chain = tryOrNull {
            KeyChain.getCertificateChain(context, envCertAlias)
        }
        if (chain == null) {
            log.debug {
                "handleClientCertRequest(): chain_getting_failed:" +
                        "\nalias=$envCertAlias"
            }

            ignoreRequest(request)
            return
        }

        proceedRequest(request, privateKey, chain)
    }

    private fun ignoreRequest(request: ClientCertRequest) {
        request.ignore()

        log.debug {
            "ignoreRequest(): request_ignored"
        }
    }

    private fun proceedRequest(
        request: ClientCertRequest,
        privateKey: PrivateKey,
        chain: Array<X509Certificate>,
    ) {
        request.proceed(privateKey, chain)

        log.debug {
            "proceedRequest(): request_proceeded"
        }
    }
}
