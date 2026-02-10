package ua.com.radiokot.photoprism.features.webview.logic

import android.graphics.Color
import androidx.annotation.ColorInt
import com.google.common.hash.Hashing

/**
 * A factory of JS to be injected to the WebView.
 *
 * @see Script
 */
class WebViewInjectionScriptFactory {

    fun getPhotoPrismAutoLoginScript(sessionId: String): String {
        // New fields for the upcoming 2FA+OpenID release are authToken and sessionId.
        // See https://github.com/photoprism/photoprism/issues/808#issuecomment-1880117188
        val newSessionId = Hashing.sha256()
            .hashString(sessionId, Charsets.UTF_8)
            .toString()

        return """
            // Reset the state on each opening.
            localStorage.clear()
            
            localStorage.setItem('session_id', '$sessionId')
            localStorage.setItem('sessionId', '$newSessionId')
            localStorage.setItem('authToken', '$sessionId')
            
            // Minimal user model is required for the app to skip the login.
            localStorage.setItem('user','{"ID":42,"UID":""}')
        """.trimIndent()
    }

    fun getPhotoPrismImmersiveScript(
        @ColorInt
        backgroundColor: Int,
    ): String =
        """
            var immersiveCss = `
                <style type="text/css">
                    /* Remove navigation paddings */
                    .v-content__wrap {
                        padding: 0px !important;
                    }
                    
                    /* Make the content background match window color */
                    .theme-default .v-content__wrap {
                        background: ${backgroundColor.toCssRgb()} !important;
                    }
                    
                    /* Make the content container background match window color.
                       This container doesn't wraps the card and needs its separate rule.
                       Making it 100vh causes the card to jump when selected. */
                    .p-page-photos .container {
                        background: ${backgroundColor.toCssRgb()} !important;
                    }
                    
                    /* Remove navigation paddings from the content */
                    .v-content {
                        padding: 0px !important;
                    }
                    
                    /* Hide toolbar navigation */
                    .v-toolbar__content {
                        display: none !important;
                    }
                    
                    /* Hide sidebar navigation */
                    #p-navigation {
                        display: none !important;
                    }
                    
                    /* Hide download buttons, they do nothing in web viewer */
                    .action-download {
                        display: none !important;
                    }
                </style>
            `
            
            document.head.insertAdjacentHTML('beforeend', immersiveCss)
        """.trimIndent()

    fun getGitHubWikiImmersiveScript(
        @ColorInt
        textColor: Int,
        @ColorInt
        backgroundColor: Int,
        @ColorInt
        primaryColor: Int,
    ): String =
        """
            function injectImmersiveCss() {
                var immersiveCss = `
                    <style type="text/css">
                        /* Hide toolbar, sidebar, wiki page header */
                        .AppHeader, .Layout-sidebar, .js-header-wrapper, 
                        #repository-container-header, .gh-header, .gh-header-meta,
                        .wiki-rightbar {
                            display: none !important;
                        }
                        
                        /* Remove content extra spacing */
                        .mt-4 {
                            margin-top: 0px !important;
                            padding-top: 4px !important;
                            padding-left: 8px !important;
                            padding-right: 8px !important;
                        }
                        
                        /* Make the background match window color */
                        html, body {
                            background: ${backgroundColor.toCssRgb()} !important;
                            color: ${textColor.toCssRgb()} !important;
                            margin-bottom: 12px;
                        }
                        
                        /* Make the links primary */
                        a {
                            color: ${primaryColor.toCssRgb()} !important;
                        }
                        
                        /* Remove heading link buttons */
                        .markdown-heading .anchor .octicon {
                            display: none;
                        }
                        
                        /* Remove the header bottom stroke */
                        .markdown-body h1, .markdown-body h2,
                        .markdown-body h3, .markdown-body h4 {
                            border-bottom: none;
                        }
                        
                        .footer {
                            margin-top: 24px;
                            display: none;
                        }
                    </style>
                `
                document.head.insertAdjacentHTML('beforeend', immersiveCss)
            }

            if (document.readyState !== 'loading') {
                injectImmersiveCss()
            } else {
                addEventListener("DOMContentLoaded", injectImmersiveCss)
            }
        """.trimIndent()

    fun getPhotoPrismHelpImmersiveScript(
        @ColorInt
        backgroundColor: Int,
        @ColorInt
        textColor: Int,
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
                            background: ${backgroundColor.toCssRgb()} !important;
                            color: ${textColor.toCssRgb()} !important;
                        }
                        
                        /* Fix table text color in dark mode */
                        table {
                            background-color: transparent !important; 
                            border-color: ${textColor.toCssRgb()} !important;
                        }
                        
                        .md-typeset table:not([class]) td {
                            border-color: ${textColor.toCssRgb()} !important;
                        }
                    </style>
                `
                document.head.insertAdjacentHTML('beforeend', immersiveCss)
            }
            
            if (document.readyState !== 'loading') {
                injectImmersiveCss()
            } else {
                addEventListener("DOMContentLoaded", injectImmersiveCss)
            }
        """.trimIndent()

    fun getSimpleHtmlImmersiveScript(
        @ColorInt
        backgroundColor: Int,
        @ColorInt
        codeBlockColor: Int,
        @ColorInt
        textColor: Int,
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
                        background: ${backgroundColor.toCssRgb()} !important;
                        color: ${textColor.toCssRgb()} !important;
                    }
                    
                    /* Make the links primary */
                    a {
                        color: ${primaryColor.toCssRgb()} !important;
                    }
                    
                    /* Make the code blocks look good
                     * in both light and dark modes
                     */
                    pre {
                        background: ${codeBlockColor.toCssRgb()} !important;
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
