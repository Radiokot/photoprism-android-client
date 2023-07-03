package ua.com.radiokot.photoprism.features.webview.view

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.activity.OnBackPressedCallback
import androidx.annotation.StringRes
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.koin.android.ext.android.inject
import ua.com.radiokot.photoprism.R
import ua.com.radiokot.photoprism.base.view.BaseActivity
import ua.com.radiokot.photoprism.databinding.ActivityWebViewBinding
import ua.com.radiokot.photoprism.extension.kLogger
import ua.com.radiokot.photoprism.extension.withMaskedCredentials
import ua.com.radiokot.photoprism.features.webview.logic.WebViewInjectionScriptFactory
import ua.com.radiokot.photoprism.features.webview.logic.WebViewWVClient

class WebViewActivity : BaseActivity() {
    private val log = kLogger("WebViewActivity")

    private val webClientFactory: WebViewWVClient.Factory by inject()

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

    private val isUrlBrowsable: Boolean
        get() = url.toHttpUrlOrNull() != null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        log.debug {
            "onCreate(): creating:" +
                    "\nurl=${url.toHttpUrlOrNull()?.withMaskedCredentials() ?: url}," +
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

        val client = webClientFactory.getClient(
            pageStartedInjectionScripts = pageStartedInjectionScripts,
            pageFinishedInjectionScripts = pageFinishedInjectionScripts,
        )
        webViewClient = client

        onBackPressedDispatcher.addCallback(object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (canGoBack()) {
                    goBack()
                } else {
                    finish()
                }
            }
        })

        setBackgroundColor(Color.TRANSPARENT)
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
        menu.findItem(R.id.open_in_browser).isVisible = isUrlBrowsable
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.open_in_browser ->
                goToBrowserAndFinish()
            R.id.close ->
                finish()
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
