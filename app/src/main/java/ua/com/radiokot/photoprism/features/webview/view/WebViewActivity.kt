package ua.com.radiokot.photoprism.features.webview.view

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Looper
import android.view.Menu
import android.view.MenuItem
import android.webkit.ClientCertRequest
import android.webkit.HttpAuthHandler
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.OnBackPressedCallback
import androidx.annotation.StringRes
import com.google.android.material.color.MaterialColors
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.koin.android.ext.android.inject
import org.koin.android.scope.AndroidScopeComponent
import org.koin.androidx.scope.createActivityScope
import org.koin.core.scope.Scope
import ua.com.radiokot.photoprism.R
import ua.com.radiokot.photoprism.base.view.BaseActivity
import ua.com.radiokot.photoprism.databinding.ActivityWebViewBinding
import ua.com.radiokot.photoprism.di.DI_SCOPE_SESSION
import ua.com.radiokot.photoprism.env.data.model.EnvSession
import ua.com.radiokot.photoprism.extension.kLogger
import ua.com.radiokot.photoprism.extension.withMaskedCredentials
import ua.com.radiokot.photoprism.features.webview.logic.WebViewClientCertRequestHandler
import ua.com.radiokot.photoprism.features.webview.logic.WebViewHttpAuthRequestHandler
import ua.com.radiokot.photoprism.features.webview.logic.WebViewInjectionScriptFactory
import kotlin.concurrent.thread

class WebViewActivity : BaseActivity(), AndroidScopeComponent {
    override val scope: Scope by lazy {
        createActivityScope().apply {
            // This allows running the viewer without the session.
            getKoin().getScopeOrNull(DI_SCOPE_SESSION)
                ?.also { linkTo(it) }
        }
    }

    private val log = kLogger("WebViewActivity")

    private val session: EnvSession? by lazy {
        scope.getOrNull()
    }
    private val webViewClientCertRequestHandler: WebViewClientCertRequestHandler? by lazy {
        scope.getOrNull()
    }
    private val webViewHttpAuthRequestHandler: WebViewHttpAuthRequestHandler? by lazy {
        scope.getOrNull()
    }
    private val webViewInjectionScriptFactory: WebViewInjectionScriptFactory by inject()

    private lateinit var view: ActivityWebViewBinding
    private val webView: WebView
        get() = view.webViewFragment.getFragment<WebViewFragment>().webView

    private val url: String by lazy {
        requireNotNull(intent.getStringExtra(URL_EXTRA)) {
            "No URI specified"
        }
    }
    private val titleRes: Int by lazy {
        requireNotNull(intent.getIntExtra(TITLE_RES_EXTRA, -1).takeIf { it > 0 }) {
            "No title resource specified"
        }
    }
    private val pageStartedInjectionScripts: Set<WebViewInjectionScriptFactory.Script> by lazy {
        intent.getIntArrayExtra(PAGE_STARTED_SCRIPTS_EXTRA)
            ?.map(WebViewInjectionScriptFactory.Script.values()::get)
            ?.toSet()
            ?: emptySet()
    }
    private val pageFinishedInjectionScripts: Set<WebViewInjectionScriptFactory.Script> by lazy {
        intent.getIntArrayExtra(PAGE_FINISHED_SCRIPTS_EXTRA)
            ?.map(WebViewInjectionScriptFactory.Script.values()::get)
            ?.toSet()
            ?: emptySet()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        log.debug {
            "onCreate(): creating:" +
                    "\nurl=${url.toHttpUrl().withMaskedCredentials()}," +
                    "\ntitle=${getString(titleRes)}," +
                    "\npageStartedInjectionScripts=${pageStartedInjectionScripts.size}," +
                    "\npageFinishedInjectionScripts=${pageFinishedInjectionScripts.size}," +
                    "\nsavedInstanceState=$savedInstanceState"
        }

        view = ActivityWebViewBinding.inflate(layoutInflater)
        setContentView(view.root)

        initToolbar()

        if (savedInstanceState == null) {
            initWebView()
            navigate(url)
        }
    }

    private fun initToolbar() {
        setSupportActionBar(view.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        setTitle(titleRes)
    }

    private fun initWebView() = with(webView) {
        val backPressedCallback = object : OnBackPressedCallback(false) {
            override fun handleOnBackPressed() {
                goBack()
            }
        }
        onBackPressedDispatcher.addCallback(backPressedCallback)

        settings.apply {
            @SuppressLint("SetJavaScriptEnabled")
            javaScriptEnabled = true
            loadsImagesAutomatically = true
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            domStorageEnabled = true
            userAgentString =
                "Mozilla/5.0 (Linux; Android 10; Google Pixel) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Mobile Safari/537.36 EdgA/114.0.1823.43"
            databaseEnabled = true
        }

        webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(webView: WebView, newProgress: Int) {
                view.progressIndicator.progress = newProgress

                if (newProgress != 100) {
                    view.progressIndicator.show()
                } else {
                    view.progressIndicator.hide()
                }
            }
        }

        webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String, favicon: Bitmap?) {
                injectScripts(pageStartedInjectionScripts)
            }

            override fun onPageFinished(view: WebView?, url: String) {
                injectScripts(pageFinishedInjectionScripts)
                backPressedCallback.isEnabled = canGoBack()
            }

            override fun onReceivedClientCertRequest(view: WebView, request: ClientCertRequest) {
                log.debug {
                    "onReceivedClientCertRequest(): handling_cert_request:" +
                            "\nhost=${request.host}," +
                            "\nhandler=$webViewClientCertRequestHandler"
                }

                val doHandle = {
                    // The handler must be called from a non-main thread.
                    webViewClientCertRequestHandler
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
                            "\nhandler=$webViewHttpAuthRequestHandler"
                }

                webViewHttpAuthRequestHandler
                    ?.handleHttpAuthRequest(view, host, handler)
                    ?: handler.cancel()
            }
        }
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
                webViewInjectionScriptFactory.getPhotoPrismAutoLoginScript(
                    sessionId = checkNotNull(session) {
                        "There must be a session to inject this script"
                    }.id,
                )

            WebViewInjectionScriptFactory.Script.PHOTOPRISM_IMMERSIVE ->
                webViewInjectionScriptFactory.getPhotoPrismImmersiveScript(
                    windowBackgroundColor = windowBackgroundColor,
                )

            WebViewInjectionScriptFactory.Script.GITHUB_WIKI_IMMERSIVE ->
                webViewInjectionScriptFactory.getGitHubWikiImmersiveScript(
                    windowBackgroundColor = windowBackgroundColor,
                )

            WebViewInjectionScriptFactory.Script.PHOTOPRISM_HELP_IMMERSIVE ->
                webViewInjectionScriptFactory.getPhotoPrismHelpImmersiveScript(
                    windowBackgroundColor = windowBackgroundColor,
                    windowTextColor = MaterialColors.getColor(
                        this,
                        com.google.android.material.R.attr.colorOnBackground,
                        Color.BLUE,
                    )
                )
        }

        webView.evaluateJavascript(scriptJs, null)

        log.debug {
            "injectScripts(): injected:" +
                    "\nscript=$script"
        }
    }

    private fun navigate(url: String) {
        webView.loadUrl(url)

        log.debug {
            "navigate(): loading_url:" +
                    "\nurl=$url"
        }
    }

    private fun goToBrowserAndFinish() {
        val url = webView.url ?: url

        log.debug {
            "goToBrowserAndFinish(): opening_url:" +
                    "\nurl=$url"
        }

        try {
            startActivity(Intent(Intent.ACTION_VIEW).setData(Uri.parse(url)))
            finish()

            log.debug {
                "goToBrowserAndFinish(): finishing"
            }
        } catch (e: Exception) {
            log.error(e) { "goToBrowserAndFinish(): opening_failed" }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.web_view, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.open_in_browser ->
                goToBrowserAndFinish()
        }
        return super.onOptionsItemSelected(item)
    }

    companion object {
        private const val URL_EXTRA = "url"
        private const val TITLE_RES_EXTRA = "title_res"
        private const val PAGE_STARTED_SCRIPTS_EXTRA = "page_started_scripts"
        private const val PAGE_FINISHED_SCRIPTS_EXTRA = "page_finished_scripts"

        /**
         * @param url URL to load
         * @param titleRes string resource for the title
         * @param pageStartedInjectionScripts scripts to inject on page start
         * @param pageFinishedInjectionScripts scripts to inject on page finish
         */
        fun getBundle(
            url: String,
            @StringRes
            titleRes: Int,
            pageStartedInjectionScripts: Set<WebViewInjectionScriptFactory.Script> = emptySet(),
            pageFinishedInjectionScripts: Set<WebViewInjectionScriptFactory.Script> = emptySet(),
        ) = Bundle().apply {
            putString(URL_EXTRA, url)
            putInt(TITLE_RES_EXTRA, titleRes)
            putIntArray(
                PAGE_STARTED_SCRIPTS_EXTRA,
                pageStartedInjectionScripts
                    .map(WebViewInjectionScriptFactory.Script::ordinal)
                    .toIntArray()
            )
            putIntArray(
                PAGE_FINISHED_SCRIPTS_EXTRA,
                pageFinishedInjectionScripts
                    .map(WebViewInjectionScriptFactory.Script::ordinal)
                    .toIntArray()
            )
        }
    }
}
