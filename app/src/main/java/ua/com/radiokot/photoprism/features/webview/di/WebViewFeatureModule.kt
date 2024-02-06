package ua.com.radiokot.photoprism.features.webview.di

import org.koin.android.ext.koin.androidApplication
import org.koin.dsl.bind
import org.koin.dsl.module
import ua.com.radiokot.photoprism.di.envModule
import ua.com.radiokot.photoprism.env.data.model.EnvSession
import ua.com.radiokot.photoprism.features.webview.logic.SessionCertWebViewClientCertRequestHandler
import ua.com.radiokot.photoprism.features.webview.logic.SessionRootUrlWebViewHttpAuthRequestHandler
import ua.com.radiokot.photoprism.features.webview.logic.WebViewClientCertRequestHandler
import ua.com.radiokot.photoprism.features.webview.logic.WebViewHttpAuthRequestHandler
import ua.com.radiokot.photoprism.features.webview.logic.WebViewInjectionScriptFactory
import ua.com.radiokot.photoprism.features.webview.logic.WebViewWVClient

val webViewFeatureModule = module {
    includes(envModule)

    single {
        WebViewInjectionScriptFactory()
    } bind WebViewInjectionScriptFactory::class

    // WebView client default implementation,
    // which doesn't have any session-related handlers.
    single {
        WebViewWVClient.Factory(
            sessionId = null,
            clientCertRequestHandler = null,
            httpAuthRequestHandler = null,
            injectionScriptFactory = get(),
        )
    } bind WebViewWVClient.Factory::class

    scope<EnvSession> {
        scoped {
            val session = get<EnvSession>()

            SessionCertWebViewClientCertRequestHandler(
                envConnectionParams = session.envConnectionParams,
                context = androidApplication(),
            )
        } bind WebViewClientCertRequestHandler::class

        scoped {
            val session = get<EnvSession>()

            SessionRootUrlWebViewHttpAuthRequestHandler(
                envRootUrl = session.envConnectionParams.rootUrl,
            )
        } bind WebViewHttpAuthRequestHandler::class

        // WebView client for session scope, which has all the handlers.
        scoped {
            val session = get<EnvSession>()

            WebViewWVClient.Factory(
                sessionId = session.id,
                clientCertRequestHandler = get(),
                httpAuthRequestHandler = get(),
                injectionScriptFactory = get(),
            )
        } bind WebViewWVClient.Factory::class
    }
}
