package ua.com.radiokot.photoprism.features.webview.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import androidx.fragment.app.Fragment

/**
 * A WebView placed into a fragment retaining its instance
 * to survive activity recreation.
 */
class WebViewFragment : Fragment() {
    private lateinit var mWebView: WebView
    val webView: WebView
        get() = mWebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Force instance retention is OK in this case,
        // it is only for this view.
        @Suppress("DEPRECATION")
        retainInstance = true

        mWebView = WebView(requireActivity())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return webView
    }
}
