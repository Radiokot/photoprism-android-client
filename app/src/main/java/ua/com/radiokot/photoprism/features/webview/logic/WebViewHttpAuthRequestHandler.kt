package ua.com.radiokot.photoprism.features.webview.logic

import android.webkit.HttpAuthHandler
import android.webkit.WebView

interface WebViewHttpAuthRequestHandler {
    fun handleHttpAuthRequest(view: WebView, host: String, handler: HttpAuthHandler)
}
