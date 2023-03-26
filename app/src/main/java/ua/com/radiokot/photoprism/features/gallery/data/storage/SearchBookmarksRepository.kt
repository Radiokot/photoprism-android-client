package ua.com.radiokot.photoprism.features.gallery.data.storage

import io.reactivex.rxjava3.core.Single
import ua.com.radiokot.photoprism.base.data.storage.SimpleCollectionRepository
import ua.com.radiokot.photoprism.extension.toSingle
import ua.com.radiokot.photoprism.features.gallery.data.model.SearchBookmark

class SearchBookmarksRepository : SimpleCollectionRepository<SearchBookmark>() {
    override fun getCollection(): Single<List<SearchBookmark>> = {
        var i = 0L
        listOf(
            SearchBookmark(name = "My Screenshots", id = ++i),
            SearchBookmark(name = "Yasya Camera", id = ++i),
            SearchBookmark(name = "My camera", id = ++i),
            SearchBookmark(name = "TikToks", id = ++i)
        )
    }.toSingle()
}