package ua.com.radiokot.photoprism.features.gallery.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class SearchConfig(
    val mediaTypes: Set<GalleryMedia.TypeName>,
    val userQuery: String?,
) : Parcelable