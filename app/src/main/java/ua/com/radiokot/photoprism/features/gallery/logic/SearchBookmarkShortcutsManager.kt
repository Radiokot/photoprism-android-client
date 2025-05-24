package ua.com.radiokot.photoprism.features.gallery.logic

import android.app.Application
import android.content.Intent
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import io.reactivex.rxjava3.disposables.Disposable
import ua.com.radiokot.photoprism.R
import ua.com.radiokot.photoprism.features.gallery.data.storage.SearchBookmarksRepository
import ua.com.radiokot.photoprism.features.gallery.view.GalleryActivity

class SearchBookmarkShortcutsManager(
    private val application: Application,
) {

    /**
     * Synchronizes app dynamic shortcuts (ones appear on icon long press)
     * with the search bookmarks.
     */
    fun syncShortcutsWithBookmarks(
        bookmarksRepository: SearchBookmarksRepository,
    ): Disposable = bookmarksRepository
        .items
        .subscribe { bookmarks ->

            ShortcutManagerCompat.setDynamicShortcuts(
                application,
                bookmarks.map { bookmark ->
                    ShortcutInfoCompat.Builder(application, bookmark.id.toString())
                        .setShortLabel(bookmark.name)
                        .setIcon(
                            IconCompat.createWithResource(
                                application,
                                R.drawable.ic_bookmark_shortcut
                            )
                        )
                        .setIntent(
                            Intent(application, GalleryActivity::class.java)
                                .setAction(GalleryActivity.ACTION_BOOKMARK_SHORTCUT)
                                .putExtra(GalleryActivity.BOOKMARK_ID_EXTRA, bookmark.id)
                        )
                        .build()
                }
            )
        }
}
