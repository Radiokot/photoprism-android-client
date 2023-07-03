package ua.com.radiokot.photoprism.features.webview.logic

import android.webkit.ClientCertRequest
import android.webkit.WebView

interface WebViewClientCertRequestHandler {
    fun handleClientCertRequest(view: WebView, request: ClientCertRequest)
}
