package ua.com.radiokot.photoprism.features.gallery.search.logic

import android.content.Context
import android.provider.Settings

class TestLabDetectorImpl(
    context: Context,
) : TestLabDetector {
    private val contentResolver = context.contentResolver

    override val isRunningInTestLab: Boolean
        get() = Settings.System.getString(contentResolver, "firebase.test.lab") == "true"
}
