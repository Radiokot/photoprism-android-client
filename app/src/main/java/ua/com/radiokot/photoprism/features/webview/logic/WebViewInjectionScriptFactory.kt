package ua.com.radiokot.photoprism.features.webview.logic

import android.graphics.Color
import androidx.annotation.ColorInt
import ua.com.radiokot.photoprism.features.webview.logic.WebViewInjectionScriptFactory.Script

/**
 * A factory of JS to be injected to the WebView.
 *
 * @see Script
 */
class WebViewInjectionScriptFactory {

    fun getPhotoPrismAutoLoginScript(sessionId: String): String =
        """
            localStorage.setItem('session_id', '$sessionId')
            
            // Minimal user model is required for the app to skip the login.
            localStorage.setItem('user','{"ID":42,"UID":""}')
        """.trimIndent()

    fun getPhotoPrismImmersiveScript(@ColorInt windowBackgroundColor: Int): String =
        """
            var immersiveCss = `
                <style type="text/css">
                    /* Make the content background match window color and remove
                       navigation paddings
                    */
                    .v-content__wrap {
                        padding: 0px !important;
                        background: ${windowBackgroundColor.toCssRgb()} !important;
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
                        background: ${windowBackgroundColor.toCssRgb()} !important;
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

    fun getGitHubWikiImmersiveScript(@ColorInt windowBackgroundColor: Int): String =
        """
            function injectImmersiveCss() {
                var immersiveCss = `
                    <style type="text/css">
                        /* Hide toolbar, sidebar, wiki page header */
                        .AppHeader, .Layout-sidebar, .js-header-wrapper, 
                        #repository-container-header, .gh-header, .gh-header-meta {
                            display: none !important;
                        }
                        
                        /* Remove content extra spacing */
                        .mt-4 {
                            margin-top: 0px !important;
                            padding-top: 4px !important;
                        }
                        
                        /* Make the background match window color */
                        body {
                            background: ${windowBackgroundColor.toCssRgb()} !important;
                        }
                    </style>
                `
                document.head.insertAdjacentHTML('beforeend', immersiveCss)
            }
            
            addEventListener("DOMContentLoaded", injectImmersiveCss)
        """.trimIndent()

    fun getPhotoPrismHelpImmersiveScript(
        @ColorInt
        windowBackgroundColor: Int,
        @ColorInt
        windowTextColor: Int,
    ): String =
        """
            function injectImmersiveCss() {
                var immersiveCss = `
                    <style type="text/css">
                        /* Hide toolbar, page header, edit button */
                        .md-header, #using-search-filters, .md-content__button   {
                            display: none !important;
                        }
                        
                        /* Remove content extra spacing */
                        article, .md-main__inner {
                            margin-top: 0px !important;
                            padding-top: 0px !important;
                        }
                        
                        /* Make the background match window color */
                        body {
                            background: ${windowBackgroundColor.toCssRgb()} !important;
                            color: ${windowTextColor.toCssRgb()} !important;
                        }
                        
                        /* Fix table text color in dark mode */
                        table {
                            background-color: transparent !important; 
                            border-color: ${windowTextColor.toCssRgb()} !important;
                        }
                        
                        .md-typeset table:not([class]) td {
                            border-color: ${windowTextColor.toCssRgb()} !important;
                        }
                    </style>
                `
                document.head.insertAdjacentHTML('beforeend', immersiveCss)
            }
            
            addEventListener("DOMContentLoaded", injectImmersiveCss)
        """.trimIndent()

    fun getSimpleHtmlImmersiveScript(
        @ColorInt
        windowBackgroundColor: Int,
        @ColorInt
        surfaceVariantColor: Int,
        @ColorInt
        windowTextColor: Int,
        @ColorInt
        primaryColor: Int,
    ): String =
        """
            var immersiveCss = `
                <style type="text/css">
                    /* Make the background match window color
                     * and the text match text color
                     */
                    body {
                        background: ${windowBackgroundColor.toCssRgb()} !important;
                        color: ${windowTextColor.toCssRgb()} !important;
                    }
                    
                    /* Make the links primary */
                    a {
                        color: ${primaryColor.toCssRgb()} !important;
                    }
                    
                    /* Make the code blocks look good
                     * in both light and dark modes
                     */
                    pre {
                        background: ${surfaceVariantColor.toCssRgb()} !important;
                    }
                </style>
            `
            document.head.insertAdjacentHTML('beforeend', immersiveCss)
        """.trimIndent()

    private fun Int.toCssRgb(): String =
        "rgb(${Color.red(this)},${Color.green(this)},${Color.blue(this)})"

    enum class Script {
        PHOTOPRISM_AUTO_LOGIN,
        PHOTOPRISM_IMMERSIVE,
        GITHUB_WIKI_IMMERSIVE,
        PHOTOPRISM_HELP_IMMERSIVE,
        SIMPLE_HTML_IMMERSIVE,
        ;
    }
}
