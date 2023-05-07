package ua.com.radiokot.photoprism.features.gallery.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.util.*

@Parcelize
data class SearchConfig(
    val mediaTypes: Set<GalleryMedia.TypeName>,
    val albumUid: String?,
    val before: Date?,
    val userQuery: String,
    val includePrivate: Boolean,
) : Parcelable {
    companion object {
        val DEFAULT = SearchConfig(
            mediaTypes = emptySet(),
            albumUid = null,
            before = null,
            userQuery = "",
            includePrivate = false,
        )
    }
}