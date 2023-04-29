package ua.com.radiokot.photoprism.features.gallery.logic

import okhttp3.HttpUrl

class PhotoPrismMediaWebUrlFactory(
    webLibraryUrl: HttpUrl,
) : MediaWebUrlFactory {
    private val browseUrlBase: HttpUrl =
        webLibraryUrl.newBuilder()
            .addPathSegment("browse")
            .build()

    override fun getWebViewUrl(uid: String): String =
        browseUrlBase.newBuilder()
            .addQueryParameter("q", "uid:$uid")
            .toString()
}