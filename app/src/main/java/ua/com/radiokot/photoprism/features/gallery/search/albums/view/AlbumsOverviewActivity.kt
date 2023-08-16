package ua.com.radiokot.photoprism.features.gallery.search.albums.view

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.Menu
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import androidx.appcompat.widget.SearchView
import androidx.core.view.doOnPreDraw
import androidx.core.widget.ImageViewCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.color.MaterialColors
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.adapters.ItemAdapter
import io.reactivex.rxjava3.kotlin.subscribeBy
import org.koin.androidx.viewmodel.ext.android.viewModel
import ua.com.radiokot.photoprism.R
import ua.com.radiokot.photoprism.base.view.BaseActivity
import ua.com.radiokot.photoprism.databinding.ActivityAlbumsOverviewBinding
import ua.com.radiokot.photoprism.extension.autoDispose
import ua.com.radiokot.photoprism.extension.bindTextTwoWay
import ua.com.radiokot.photoprism.extension.kLogger
import ua.com.radiokot.photoprism.features.gallery.search.albums.view.model.AlbumListItem
import ua.com.radiokot.photoprism.features.gallery.search.albums.view.model.AlbumsOverviewViewModel
import ua.com.radiokot.photoprism.view.ErrorView

class AlbumsOverviewActivity : BaseActivity() {
    private val log = kLogger("AlbumsOverviewActivity")

    private lateinit var view: ActivityAlbumsOverviewBinding
    private val viewModel: AlbumsOverviewViewModel by viewModel()
    private val adapter = ItemAdapter<AlbumListItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        view = ActivityAlbumsOverviewBinding.inflate(layoutInflater)
        setContentView(view.root)

        setSupportActionBar(view.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Init the list once it is laid out.
        view.albumsRecyclerView.doOnPreDraw {
            initList()
        }
        initErrorView()

        subscribeToState()
    }

    private fun initList() {
        val albumsAdapter = FastAdapter.with(adapter).apply {
            stateRestorationPolicy = RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY

            onClickListener = { _, _, item: AlbumListItem, _ ->
                // TODO
                true
            }
        }

        with(view.albumsRecyclerView) {
            // Safe dimensions of the list keeping from division by 0.
            // The fallback size is not supposed to be taken,
            // as it means initializing of a not laid out list.
            val listWidth = measuredWidth
                .takeIf { it > 0 }
                ?: FALLBACK_LIST_SIZE
                    .also {
                        log.warn { "initList(): used_fallback_width" }
                    }

            val minItemWidthPx =
                resources.getDimensionPixelSize(R.dimen.list_item_album_width)
            val spanCount = (listWidth / minItemWidthPx).coerceAtLeast(1)

            log.debug {
                "initList(): calculated_grid:" +
                        "\nspanCount=$spanCount," +
                        "\nrowWidth=$listWidth," +
                        "\nminItemWidthPx=$minItemWidthPx"
            }

            adapter = albumsAdapter
            layoutManager = GridLayoutManager(context, spanCount)
        }
    }

    private fun initErrorView() {
        view.errorView.replaces(view.albumsRecyclerView)
    }

    private fun subscribeToState() = viewModel.state.subscribeBy { state ->
        log.debug {
            "subscribeToState(): received_new_state:" +
                    "\nstate=$state"
        }

        adapter.setNewList(
            when (state) {
                is AlbumsOverviewViewModel.State.Ready ->
                    state.albums

                else ->
                    emptyList()
            }
        )

        // Error view.
        when (state) {
            AlbumsOverviewViewModel.State.LoadingFailed -> {
                view.errorView.showError(ErrorView.Error.General(
                    context = view.errorView.context,
                    messageRes = R.string.failed_to_load_albums,
                    retryButtonTextRes = R.string.try_again,
                    retryButtonClickListener = {
                        // TODO add
                    }
                ))
            }

            is AlbumsOverviewViewModel.State.Ready -> {
                if (state.albums.isEmpty()) {
                    view.errorView.showError(
                        ErrorView.Error.EmptyView(
                            context = view.errorView.context,
                            messageRes = R.string.no_albums_found,
                        )
                    )
                } else {
                    view.errorView.hide()
                }
            }

            else -> {
                view.errorView.hide()
            }
        }

        log.debug {
            "subscribeToState(): handled_new_state:" +
                    "\nstate=$state"
        }
    }.autoDispose(this)


    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.albums, menu)

        // Set up the search.
        with(menu?.findItem(R.id.search_view)?.actionView as SearchView) {
            queryHint = getString(R.string.enter_the_query)

            // Apply the correct color for the search close button.
            with(findViewById<ImageView>(androidx.appcompat.R.id.search_close_btn)) {
                ImageViewCompat.setImageTintList(
                    this, ColorStateList.valueOf(
                        MaterialColors.getColor(
                            view.toolbar, com.google.android.material.R.attr.colorOnSurfaceVariant
                        )
                    )
                )
            }

            // Remove the underline.
            with(findViewById<View>(androidx.appcompat.R.id.search_plate)) {
                background = null
            }

            // Directly bind the input.
            findViewById<EditText>(androidx.appcompat.R.id.search_src_text)
                .bindTextTwoWay(viewModel.filterInput, this@AlbumsOverviewActivity)
        }

        return super.onCreateOptionsMenu(menu)
    }

    private companion object {
        private const val FALLBACK_LIST_SIZE = 100
    }
}
