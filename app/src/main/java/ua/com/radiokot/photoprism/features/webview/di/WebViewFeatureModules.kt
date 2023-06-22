package ua.com.radiokot.photoprism.features.webview.di

import org.koin.android.ext.koin.androidApplication
import org.koin.core.module.Module
import org.koin.dsl.bind
import org.koin.dsl.module
import ua.com.radiokot.photoprism.di.envModules
import ua.com.radiokot.photoprism.env.data.model.EnvSession
import ua.com.radiokot.photoprism.features.webview.logic.SessionCertWebViewClientCertRequestHandler
import ua.com.radiokot.photoprism.features.webview.logic.SessionRootUrlWebViewHttpAuthRequestHandler
import ua.com.radiokot.photoprism.features.webview.logic.WebViewClientCertRequestHandler
import ua.com.radiokot.photoprism.features.webview.logic.WebViewHttpAuthRequestHandler

val webViewFeatureModules: List<Module> = listOf(
    module {
        includes(envModules)

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
        }
    }
)
