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
            // Override default quality setting of the demo env.
            .addQueryParameter("q", "uid:$uid quality:0 public:false")
            // Force include private.
            .addQueryParameter("public", "false")
            // Force "cards" view for immersion (big thumbnail with EXIF data).
            .addQueryParameter("view", "cards")
            .toString()
}
