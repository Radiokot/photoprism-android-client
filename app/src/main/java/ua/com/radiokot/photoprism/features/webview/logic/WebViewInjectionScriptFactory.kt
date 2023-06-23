package ua.com.radiokot.photoprism.features.webview.logic

import android.graphics.Color
import androidx.annotation.ColorInt
import ua.com.radiokot.photoprism.features.webview.logic.WebViewInjectionScriptFactory.Script

/**
 * A factory of JS to be injected to the WebView.
 *
 * @see Script
 */
class WebViewInjectionScriptFactory() {

    fun getPhotoPrismAutoLoginScript(sessionId: String): String =
        """
            localStorage.setItem('session_id', '$sessionId')
            
            // Minimal user model is required for the app to skip the login.
            localStorage.setItem('user','{"ID":42,"UID":""}')
        """.trimIndent()

    fun getPhotoPrismImmersiveScript(@ColorInt windowBackgroundColor: Int): String {
        val windowBackgroundColorRgb = windowBackgroundColor.let {
            "rgb(${Color.red(it)},${Color.green(it)},${Color.blue(it)})"
        }

        return """
            const immersiveCss = `
                <style type="text/css">
                    /* Make the content background match window color and remove
                       navigation paddings
                    */
                    .v-content__wrap {
                        padding: 0px !important;
                        background: $windowBackgroundColorRgb !important;
                    }
                    
                    /* Remove navigation paddings from the content */
                    .v-content {
                        padding: 0px !important;
                    }
                    
                    /* Hide toolbar navigation */
                    .v-toolbar__content {
                        display: none !important;
                    }
                    
                    /* Make the content container background match window color and fill the height*/
                    .p-page-photos .container {
                        background: $windowBackgroundColorRgb !important;
                        min-height: 100vh !important;    
                    }
                    
                    /* Hide sidebar navigation */
                    #p-navigation {
                        display: none !important;
                    }
                </style>
            `
            
            document.head.insertAdjacentHTML('beforeend', immersiveCss)
        """.trimIndent()
    }

    enum class Script {
        PHOTOPRISM_AUTO_LOGIN,
        PHOTOPRISM_IMMERSIVE,
        ;
    }
}
