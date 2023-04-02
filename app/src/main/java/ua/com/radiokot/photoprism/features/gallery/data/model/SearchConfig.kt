package ua.com.radiokot.photoprism.features.gallery.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.util.*

@Parcelize
data class SearchConfig(
    val mediaTypes: Set<GalleryMedia.TypeName>,
    val before: Date?,
    val userQuery: String?,
) : Parcelable