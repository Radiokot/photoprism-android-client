package ua.com.radiokot.photoprism.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabsClient
import androidx.browser.customtabs.CustomTabsIntent
import ua.com.radiokot.photoprism.extension.kLogger

object CustomTabsHelper {
    private val log = kLogger("CustomTabsHelper")

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
     * on failure starts [Intent.ACTION_VIEW] intent,
     * on failure returns false.
     */
    fun safelyLaunchUrl(
        context: Context,
        intent: CustomTabsIntent,
        url: Uri
    ): Boolean {
        return try {
            intent.launchUrl(context, url)
            true
        } catch (e1: Exception) {
            log.debug(e1) { "safelyLaunchUrl(): falling_back_to_regular_intent" }

            try {
                context.startActivity(Intent(Intent.ACTION_VIEW).setData(url))
                true
            } catch (e2: Exception) {
                log.error(e2) { "openUrl(): fallback_failed_too" }
                false
            }
        }
    }
}