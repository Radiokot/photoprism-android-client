package ua.com.radiokot.photoprism.features.webview.logic

import android.graphics.Bitmap
import android.os.Looper
import android.webkit.ClientCertRequest
import android.webkit.HttpAuthHandler
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.OnBackPressedCallback
import androidx.annotation.ColorInt
import ua.com.radiokot.photoprism.extension.kLogger
import ua.com.radiokot.photoprism.features.webview.logic.WebViewWVClient.Factory
import kotlin.concurrent.thread

/**
 * [WebViewClient] for the internal web-viewer.
 * It enables mTLS and HTTP basic auth, does script injection,
 * provides a back pressed callback for the "go back" action.
 *
 * @see Factory
 * @see goBackBackPressedCallback
 */
class WebViewWVClient(
    private val clientCertRequestHandler: WebViewClientCertRequestHandler?,
    private val httpAuthRequestHandler: WebViewHttpAuthRequestHandler?,
    private val injectionScriptFactory: WebViewInjectionScriptFactory,
    private val pageStartedInjectionScripts: Set<WebViewInjectionScriptFactory.Script>,
    private val pageFinishedInjectionScripts: Set<WebViewInjectionScriptFactory.Script>,
    private val sessionId: String?,
    @ColorInt
    private val windowBackgroundColor: Int,
    @ColorInt
    private val windowTextColor: Int,
) : WebViewClient() {
    private val log = kLogger("WebViewWVClient")

    private lateinit var webView: WebView

    val goBackBackPressedCallback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            webView.goBack()
        }
    }

    override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
        webView = view
        injectScripts(pageStartedInjectionScripts)
    }

    override fun onPageFinished(view: WebView, url: String) {
        webView = view

        injectScripts(pageFinishedInjectionScripts)
        goBackBackPressedCallback.isEnabled = view.canGoBack()
    }

    override fun onReceivedClientCertRequest(view: WebView, request: ClientCertRequest) {
        log.debug {
            "onReceivedClientCertRequest(): handling_cert_request:" +
                    "\nhost=${request.host}," +
                    "\nhandler=$clientCertRequestHandler"
        }

        val doHandle = {
            // The handler must be called from a non-main thread.
            clientCertRequestHandler
                ?.handleClientCertRequest(view, request)
                ?: request.cancel()
        }

        // Sometimes it is, sometimes it is not.
        if (Thread.currentThread() == Looper.getMainLooper().thread) {
            thread(block = doHandle)
        } else {
            doHandle()
        }
    }

    override fun onReceivedHttpAuthRequest(
        view: WebView,
        handler: HttpAuthHandler,
        host: String,
        realm: String
    ) {
        log.debug {
            "onReceivedHttpAuthRequest(): handling_http_auth_request:" +
                    "\nhost=$host," +
                    "\nhandler=$httpAuthRequestHandler"
        }

        httpAuthRequestHandler
            ?.handleHttpAuthRequest(view, host, handler)
            ?: handler.cancel()
    }

    private fun injectScripts(
        scripts: Set<WebViewInjectionScriptFactory.Script>
    ) = scripts.forEach { script ->
        log.debug {
            "injectScripts(): injecting:" +
                    "\nscript=$script"
        }

        val scriptJs = when (script) {
            WebViewInjectionScriptFactory.Script.PHOTOPRISM_AUTO_LOGIN ->
                injectionScriptFactory.getPhotoPrismAutoLoginScript(
                    sessionId = checkNotNull(sessionId) {
                        "There must be a session to inject this script"
                    },
                )

            WebViewInjectionScriptFactory.Script.PHOTOPRISM_IMMERSIVE ->
                injectionScriptFactory.getPhotoPrismImmersiveScript(
                    windowBackgroundColor = windowBackgroundColor,
                )

            WebViewInjectionScriptFactory.Script.GITHUB_WIKI_IMMERSIVE ->
                injectionScriptFactory.getGitHubWikiImmersiveScript(
                    windowBackgroundColor = windowBackgroundColor,
                )

            WebViewInjectionScriptFactory.Script.PHOTOPRISM_HELP_IMMERSIVE ->
                injectionScriptFactory.getPhotoPrismHelpImmersiveScript(
                    windowBackgroundColor = windowBackgroundColor,
                    windowTextColor = windowTextColor,
                )
        }

        webView.evaluateJavascript(scriptJs, null)

        log.debug {
            "injectScripts(): injected:" +
                    "\nscript=$script"
        }
    }

    class Factory(
        private val sessionId: String?,
        private val clientCertRequestHandler: WebViewClientCertRequestHandler?,
        private val httpAuthRequestHandler: WebViewHttpAuthRequestHandler?,
        private val injectionScriptFactory: WebViewInjectionScriptFactory,
    ) {
        fun getClient(
            pageStartedInjectionScripts: Set<WebViewInjectionScriptFactory.Script>,
            pageFinishedInjectionScripts: Set<WebViewInjectionScriptFactory.Script>,
            @ColorInt
            windowBackgroundColor: Int,
            @ColorInt
            windowTextColor: Int,
        ): WebViewWVClient = WebViewWVClient(
            clientCertRequestHandler = clientCertRequestHandler,
            httpAuthRequestHandler = httpAuthRequestHandler,
            injectionScriptFactory = injectionScriptFactory,
            pageStartedInjectionScripts = pageStartedInjectionScripts,
            pageFinishedInjectionScripts = pageFinishedInjectionScripts,
            sessionId = sessionId,
            windowBackgroundColor = windowBackgroundColor,
            windowTextColor = windowTextColor,
        )
    }
}
