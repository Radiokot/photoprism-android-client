package ua.com.radiokot.photoprism.features.webview.view

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.webkit.ClientCertRequest
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.OnBackPressedCallback
import androidx.annotation.StringRes
import com.google.android.material.color.MaterialColors
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.kotlin.toCompletable
import io.reactivex.rxjava3.schedulers.Schedulers
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.koin.android.ext.android.inject
import org.koin.android.scope.AndroidScopeComponent
import org.koin.androidx.scope.createActivityScope
import org.koin.core.scope.Scope
import ua.com.radiokot.photoprism.base.view.BaseActivity
import ua.com.radiokot.photoprism.databinding.ActivityWebViewBinding
import ua.com.radiokot.photoprism.di.DI_SCOPE_SESSION
import ua.com.radiokot.photoprism.env.data.model.EnvSession
import ua.com.radiokot.photoprism.extension.autoDispose
import ua.com.radiokot.photoprism.extension.kLogger
import ua.com.radiokot.photoprism.extension.withMaskedCredentials
import ua.com.radiokot.photoprism.features.webview.logic.WebViewClientCertRequestHandler

class WebViewActivity : BaseActivity(), AndroidScopeComponent {
    override val scope: Scope by lazy {
        createActivityScope().apply {
            linkTo(getScope(DI_SCOPE_SESSION))
        }
    }

    private val log = kLogger("WebViewActivity")

    private val session: EnvSession by inject()
    private val webViewClientCertRequestHandler: WebViewClientCertRequestHandler by inject()

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        log.debug {
            "onCreate(): creating:" +
                    "\nurl=${url.toHttpUrl().withMaskedCredentials()}," +
                    "\ntitle=${getString(titleRes)}," +
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
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                injectScriptOnce()
                backPressedCallback.isEnabled = canGoBack()
            }

            override fun onReceivedClientCertRequest(view: WebView, request: ClientCertRequest) {
                log.debug {
                    "onReceivedClientCertRequest(): handling_cert_request:" +
                            "\nhost=${request.host}," +
                            "\nhandler=$webViewClientCertRequestHandler"
                }

                handleClientCertRequest(view, request)
            }
        }
    }

    private var isScriptInjected = false
    private fun injectScriptOnce() {
        if (isScriptInjected) {
            return
        }

        val backgroundColor = MaterialColors.getColor(
            this,
            android.R.attr.colorBackground,
            Color.RED
        )
        val backgroundColorRgb =
            "rgb(${Color.red(backgroundColor)},${Color.green(backgroundColor)},${
                Color.blue(backgroundColor)
            })"


        webView.evaluateJavascript(
            """
                    localStorage.setItem('session_id', '${session.id}')
                    localStorage.setItem('user','{"ID":42}')
                    
                    const immersiveCss = `
                        <style type="text/css">
                            .v-content__wrap {
                                padding-top: 0px !important;
                                background: $backgroundColorRgb !important;
                            }
                            .v-toolbar__content {
                                display: none !important;
                            }
                            .p-page-photos .container {
                                background: $backgroundColorRgb !important;
                                min-height: 100vh !important;    
                            }
                        </style>
                    `
                    
                    document.head.insertAdjacentHTML('beforeend', immersiveCss)
                """.trimIndent(), null
        )

        isScriptInjected = true

        log.debug { "injectScriptOnce(): script_injected" }
    }

    private var clientCertRequestHandlingDisposable: Disposable? = null
    private fun handleClientCertRequest(view: WebView, request: ClientCertRequest) {
        clientCertRequestHandlingDisposable?.dispose()
        clientCertRequestHandlingDisposable = {
            // This needs to be done in a non-main thread.
            webViewClientCertRequestHandler.handleClientCertRequest(view, request)
        }
            .toCompletable()
            .subscribeOn(Schedulers.io())
            .subscribeBy()
            .autoDispose(this)
    }

    private fun navigate(url: String) {
        webView.loadUrl(url)

        log.debug {
            "navigate(): loading_url:" +
                    "\nurl=$url"
        }
    }

    companion object {
        private const val URL_EXTRA = "url"
        private const val TITLE_RES_EXTRA = "title_res"

        /**
         * @param url URL to load
         * @param titleRes string resource for the title
         */
        fun getBundle(
            url: String,
            @StringRes
            titleRes: Int,
        ) = Bundle().apply {
            putString(URL_EXTRA, url)
            putInt(TITLE_RES_EXTRA, titleRes)
        }
    }
}
