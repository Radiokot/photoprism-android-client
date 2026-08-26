package ua.com.radiokot.photoprism.extension

import androidx.core.graphics.Insets
import androidx.core.view.WindowInsetsCompat

fun WindowInsetsCompat.barsAndCutout(): Insets =
    getInsets(
        WindowInsetsCompat.Type.displayCutout() or
                WindowInsetsCompat.Type.systemBars()
    )

fun WindowInsetsCompat.ime(): Insets =
    getInsets(
        WindowInsetsCompat.Type.ime()
    )
