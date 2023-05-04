package ua.com.radiokot.photoprism.features.gallery.view.model

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import ua.com.radiokot.photoprism.R
import ua.com.radiokot.photoprism.features.gallery.data.model.Album

object AlbumTypeResources {
    @DrawableRes
    fun getIcon(typeName: Album.TypeName): Int = when (typeName) {
        Album.TypeName.ALBUM ->
            R.drawable.ic_album
        Album.TypeName.FOLDER ->
            R.drawable.ic_folder
        Album.TypeName.MOMENT ->
            R.drawable.ic_unknown //!!! CHANGE !!!
        Album.TypeName.MONTH ->
            R.drawable.ic_unknown //!!! CHANGE !!!
        /*
        Album.TypeName.STATE ->
            R.drawable.ic_camera_viewfinder
        */
    }

    @StringRes
    fun getName(typeName: Album.TypeName): Int = when (typeName) {
        Album.TypeName.ALBUM ->
            R.string.album_type_album
        Album.TypeName.FOLDER ->
            R.string.album_type_folder
        Album.TypeName.MOMENT ->
            R.string.album_type_moment
        Album.TypeName.MONTH ->
            R.string.album_type_month
        /*
        Album.TypeName.STATE ->
            R.string.album_type_state
        */
    }
}