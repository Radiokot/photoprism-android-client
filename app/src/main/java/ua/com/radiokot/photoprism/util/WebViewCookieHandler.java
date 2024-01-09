package ua.com.radiokot.photoprism.util;

import android.webkit.CookieManager;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;

/**
 * Provides a synchronization point between the WebView cookie store and okhttp3.OkHttpClient cookie store
 *
 * @author Hung Pham Sy
 * @see <a href="https://gist.github.com/hungps/8cb6d8484bb20e47d241cc8e117fa705">Source Gist</a>
 */
public final class WebViewCookieHandler implements CookieJar {
    private final CookieManager webViewCookieManager;

    public WebViewCookieHandler(CookieManager webViewCookieManager) {
        this.webViewCookieManager = webViewCookieManager;
    }

    @Override
    public void saveFromResponse(HttpUrl url, List<Cookie> cookies) {
        String urlString = url.toString();

        for (Cookie cookie : cookies) {
            webViewCookieManager.setCookie(urlString, cookie.toString());
        }
    }

    @NonNull
    @Override
    public List<Cookie> loadForRequest(HttpUrl url) {
        String urlString = url.toString();
        String cookiesString = webViewCookieManager.getCookie(urlString);

        if (cookiesString != null && !cookiesString.isEmpty()) {
            //We can split on the ';' char as the cookie manager only returns cookies
            //that match the url and haven't expired, so the cookie attributes aren't included
            String[] cookieHeaders = cookiesString.split(";");
            List<Cookie> cookies = new ArrayList<>(cookieHeaders.length);

            for (String header : cookieHeaders) {
                cookies.add(Cookie.parse(url, header));
            }

            return cookies;
        }

        return Collections.emptyList();
    }
}
