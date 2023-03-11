package ua.com.radiokot.photoprism.features.gallery.view

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import org.koin.android.ext.android.getKoin
import org.koin.core.scope.Scope
import ua.com.radiokot.photoprism.databinding.ActivityGalleryBinding
import ua.com.radiokot.photoprism.extension.kLogger

class GalleryActivity : AppCompatActivity() {
    private val sessionScope: Scope =
        getKoin().getScope("session")

    private lateinit var view: ActivityGalleryBinding
    private val viewModel: GalleryViewModel by sessionScope.inject()
    private val log = kLogger("GalleryActivity")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        view = ActivityGalleryBinding.inflate(layoutInflater)
        setContentView(view.root)

        viewModel.isLoading
            .observe(this) {
                view.isLoadingTextView.text = it.toString()
            }
    }
}