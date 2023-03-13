package ua.com.radiokot.photoprism.features.viewer.view

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.adapters.ItemAdapter
import org.koin.android.scope.AndroidScopeComponent
import org.koin.androidx.scope.createActivityScope
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.scope.Scope
import ua.com.radiokot.photoprism.databinding.ActivityMediaViewerBinding
import ua.com.radiokot.photoprism.extension.checkNotNull
import ua.com.radiokot.photoprism.extension.kLogger
import ua.com.radiokot.photoprism.features.viewer.view.model.MediaViewerPageItem

class MediaViewerActivity : AppCompatActivity(), AndroidScopeComponent {
    override val scope: Scope by lazy {
        createActivityScope().apply {
            linkTo(getScope("session"))
        }
    }

    private lateinit var view: ActivityMediaViewerBinding
    private val viewModel: MediaViewerViewModel by viewModel()
    private val log = kLogger("MMediaViewerActivity")

    private val viewerPagesAdapter = ItemAdapter<MediaViewerPageItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        view = ActivityMediaViewerBinding.inflate(layoutInflater)
        setContentView(view.root)

        supportActionBar?.hide()

        val mediaIndex = intent.getIntExtra(MEDIA_INDEX_KEY, -1)
            .takeIf { it >= 0 }
            .checkNotNull {
                "Missing media index"
            }

        val repositoryKey = intent.getStringExtra(REPO_KEY_KEY)
            .checkNotNull {
                "Missing repository key"
            }

        log.debug {
            "onCreate(): creating:" +
                    "\nmediaIndex=$mediaIndex," +
                    "\nrepositoryKey=$repositoryKey," +
                    "\nsavedInstanceState=$savedInstanceState"
        }

        viewModel.init(repositoryKey)

        subscribeToData()

        view.viewPager.post {
            initPager(mediaIndex)
        }
    }

    private fun initPager(startIndex: Int) {
        with(view.viewPager) {
            val fastAdapter = FastAdapter.with(viewerPagesAdapter).apply {
                stateRestorationPolicy =
                    RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
            }

            adapter = fastAdapter

            // TODO: Endless scrolling

            post {
                setCurrentItem(startIndex, false)
            }
        }
    }

    private fun subscribeToData() {
        viewModel.isLoading
            .observe(this) { isLoading ->
                // TODO: Show loading
                log.debug {
                    "subscribeToData(): loading_changed:" +
                            "\nis_loading=$isLoading"
                }
            }

        viewModel.itemsList
            .observe(this) {
                if (it != null) {
                    viewerPagesAdapter.setNewList(it)
                }
            }
    }

    companion object {
        private const val MEDIA_INDEX_KEY = "media-index"
        private const val REPO_KEY_KEY = "repo-key"

        fun getBundle(
            mediaIndex: Int,
            repositoryKey: String,
        ) = Bundle().apply {
            putInt(MEDIA_INDEX_KEY, mediaIndex)
            putString(REPO_KEY_KEY, repositoryKey)
        }
    }
}