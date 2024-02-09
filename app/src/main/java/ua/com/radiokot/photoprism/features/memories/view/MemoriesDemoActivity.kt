package ua.com.radiokot.photoprism.features.memories.view

import android.content.Intent
import android.os.Bundle
import androidx.recyclerview.widget.RecyclerView
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.adapters.ItemAdapter
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.schedulers.Schedulers
import org.koin.android.ext.android.get
import org.koin.android.ext.android.inject
import ua.com.radiokot.photoprism.base.view.BaseActivity
import ua.com.radiokot.photoprism.databinding.ActivityMemoriesDemoBinding
import ua.com.radiokot.photoprism.extension.autoDispose
import ua.com.radiokot.photoprism.extension.setThrottleOnClickListener
import ua.com.radiokot.photoprism.features.gallery.data.storage.SimpleGalleryMediaRepository
import ua.com.radiokot.photoprism.features.memories.data.storage.MemoriesRepository
import ua.com.radiokot.photoprism.features.memories.logic.UpdateMemoriesUseCase
import ua.com.radiokot.photoprism.features.memories.view.model.MemoryListItem
import ua.com.radiokot.photoprism.features.viewer.view.MediaViewerActivity

/**
 * This is only for demo purposes.
 */
class MemoriesDemoActivity : BaseActivity() {
    private lateinit var view: ActivityMemoriesDemoBinding

    private val repository: MemoriesRepository by inject()
    private val adapter = ItemAdapter<MemoryListItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        view = ActivityMemoriesDemoBinding.inflate(layoutInflater)
        setContentView(view.root)

        setSupportActionBar(view.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        subscribeToRepository()
        initList()

        repository.update()

        view.clearButton.setThrottleOnClickListener {
            repository.clear()
                .subscribeBy()
                .autoDispose(this)
        }
        
        view.updateNowButton.setThrottleOnClickListener {
            get<UpdateMemoriesUseCase>()
                .invoke()
                .subscribeOn(Schedulers.io())
                .subscribeBy()
                .autoDispose(this)
        }
    }

    private fun subscribeToRepository() {
        repository
            .items
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy { memories ->
                adapter.set(memories.map(::MemoryListItem))
            }
            .autoDispose(this)
    }

    private fun initList() {
        val listAdapter = FastAdapter.with(adapter).apply {
            stateRestorationPolicy = RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY

            onClickListener = { _, _, _, position: Int ->
                startActivity(
                    Intent(this@MemoriesDemoActivity, MediaViewerActivity::class.java)
                        .putExtras(
                            MediaViewerActivity.getBundle(
                                mediaIndex = 0,
                                repositoryParams = SimpleGalleryMediaRepository.Params(
                                    query = repository.itemsList[position].searchQuery,
                                ),
                                areActionsEnabled = true
                            )
                        )
                )
                true
            }
        }

        with(view.memoriesRecyclerView) {
            adapter = listAdapter
            // Layout manager is set in XML.
        }
    }
}
