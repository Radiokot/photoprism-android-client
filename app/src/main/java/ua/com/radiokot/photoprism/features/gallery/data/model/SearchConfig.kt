package ua.com.radiokot.photoprism.features.gallery.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.util.*

@Parcelize
data class SearchConfig(
    /**
     * An optional set of media types to limit the search.
     * If **null**, there is no limit on media types.
     * If **empty**, no media types are allowed and the search will find nothing.
     */
    val mediaTypes: Set<GalleryMedia.TypeName>?,
    val albumUid: String?,
    val before: Date?,
    val userQuery: String,
    val includePrivate: Boolean,
) : Parcelable {
    /**
     * @return copy of the config which doesn't overcome the allowed media types,
     * or the current instance if there is no specific allowance.
     *
     * @param allowedMediaTypes a non-empty set of the allowed media types,
     * or null if there is no specific allowance (all the types are allowed).
     */
    fun withOnlyAllowedMediaTypes(allowedMediaTypes: Set<GalleryMedia.TypeName>?): SearchConfig {
        require(allowedMediaTypes == null || allowedMediaTypes.isNotEmpty()) {
            "The set of allowed types must either be null or not empty"
        }

        return if (allowedMediaTypes != null)
            if (mediaTypes != null)
                copy(
                    mediaTypes = mediaTypes.intersect(allowedMediaTypes),
                )
            else
                copy(
                    mediaTypes = allowedMediaTypes
                )
        else
            this
    }


    companion object {
        val DEFAULT = SearchConfig(
            mediaTypes = null,
            albumUid = null,
            before = null,
            userQuery = "",
            includePrivate = false,
        )
    }
}