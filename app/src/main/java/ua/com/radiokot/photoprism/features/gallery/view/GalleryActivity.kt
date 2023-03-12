package ua.com.radiokot.photoprism.features.gallery.view

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.adapters.ItemAdapter
import org.koin.android.ext.android.getKoin
import org.koin.core.scope.Scope
import ua.com.radiokot.photoprism.databinding.ActivityGalleryBinding
import ua.com.radiokot.photoprism.extension.kLogger
import ua.com.radiokot.photoprism.features.gallery.view.model.GalleryMediaListItem

class GalleryActivity : AppCompatActivity() {
    private val sessionScope: Scope =
        getKoin().getScope("session")

    private lateinit var view: ActivityGalleryBinding
    private val viewModel: GalleryViewModel by sessionScope.inject()
    private val log = kLogger("GalleryActivity")

    private val galleryItemAdapter = ItemAdapter<GalleryMediaListItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        view = ActivityGalleryBinding.inflate(layoutInflater)
        setContentView(view.root)

        initList()

        viewModel.isLoading
            .observe(this) {
                view.isLoadingTextView.text = it.toString()
            }

        viewModel.itemsList
            .observe(this) {
                if (it != null) {
                    galleryItemAdapter.set(it)
                }
            }
    }

    private fun initList() {
        val fastAdapter = FastAdapter.with(galleryItemAdapter)
        fastAdapter.onClickListener = { _, _, item, _ ->
            log.debug {
                "list_item_clicked:" +
                        "\nsource=${item.source}"
            }
            false
        }

        with(view.galleryRecyclerView) {
            layoutManager = GridLayoutManager(context, 3)
            adapter = fastAdapter
        }
    }
}