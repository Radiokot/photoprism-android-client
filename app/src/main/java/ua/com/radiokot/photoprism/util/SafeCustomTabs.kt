package ua.com.radiokot.photoprism.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.annotation.StringRes
import androidx.browser.customtabs.CustomTabsClient
import androidx.browser.customtabs.CustomTabsIntent
import ua.com.radiokot.photoprism.extension.kLogger
import ua.com.radiokot.photoprism.features.webview.view.WebViewActivity


object SafeCustomTabs {
    private val log = kLogger("SafeCustomTabs")

    /**
     * @see CustomTabsClient.connectAndInitialize
     */
    fun safelyConnectAndInitialize(
        context: Context,
        packageName: String = "com.android.chrome"
    ): Boolean {
        return try {
            CustomTabsClient.connectAndInitialize(context, packageName)
        } catch (e: Exception) {
            log.error(e) { "safelyConnectAndInitialize(): error_occurred" }
            false
        }
    }

    /**
     * Tries to launch the [url] with help of the [intent],
     * on failure starts [WebViewActivity].
     *
     * @param context context capable of starting activities
     * @param titleRes title resource for [WebViewActivity] in case of fallback
     */
    fun launchWithFallback(
        context: Context,
        intent: CustomTabsIntent,
        url: String,
        @StringRes
        titleRes: Int,
    ) {
        try {
            val customTabsPackageName =
                checkNotNull(CustomTabsClient.getPackageName(context, emptyList())) {
                    "There must be a browser supporting Custom Tabs"
                }

            log.debug {
                "launchWithFallback(): launching_url:" +
                        "\nurl=$url," +
                        "\ncustomTabsPackageName=$customTabsPackageName"
            }

            intent.launchUrl(context, Uri.parse(url))
        } catch (e1: Exception) {
            log.debug(e1) { "launchWithFallback(): falling_back_to_web_viewer" }

            context.startActivity(
                Intent(context, WebViewActivity::class.java)
                    .putExtras(
                        WebViewActivity.getBundle(
                            url = url,
                            titleRes = titleRes,
                        )
                    )
            )
        }
    }
}
