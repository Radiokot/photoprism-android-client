package ua.com.radiokot.photoprism.features.viewer.view

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.adapters.ItemAdapter
import org.koin.android.scope.AndroidScopeComponent
import org.koin.androidx.scope.createActivityScope
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

        log.debug {
            "onCreate(): creating:" +
                    "\nmediaIndex=$mediaIndex," +
                    "\nsavedInstanceState=$savedInstanceState"
        }

        viewerPagesAdapter.setNewList(
            IntRange(
                start = (mediaIndex - 50).coerceAtLeast(0),
                endInclusive = mediaIndex + 50
            ).map {
                MediaViewerPageItem(it)
            })

        initPager(mediaIndex)
    }

    private fun initPager(startIndex: Int) {
        with(view.viewPager) {
            val fastAdapter = FastAdapter.with(viewerPagesAdapter).apply {
                stateRestorationPolicy =
                    RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
            }

            adapter = fastAdapter

            post {
                setCurrentItem(startIndex, false)
            }
        }
    }

    companion object {
        private const val MEDIA_INDEX_KEY = "media-index"

        fun getBundle(mediaIndex: Int) = Bundle().apply {
            putInt(MEDIA_INDEX_KEY, mediaIndex)
        }
    }
}