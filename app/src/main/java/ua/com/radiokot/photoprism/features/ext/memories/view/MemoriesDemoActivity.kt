package ua.com.radiokot.photoprism.features.ext.memories.view

import android.os.Bundle
import android.widget.Toast
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.schedulers.Schedulers
import org.koin.android.ext.android.get
import org.koin.android.ext.android.inject
import ua.com.radiokot.photoprism.R
import ua.com.radiokot.photoprism.base.view.BaseActivity
import ua.com.radiokot.photoprism.databinding.ActivityMemoriesDemoBinding
import ua.com.radiokot.photoprism.extension.autoDispose
import ua.com.radiokot.photoprism.extension.setThrottleOnClickListener
import ua.com.radiokot.photoprism.features.ext.memories.data.model.Memory
import ua.com.radiokot.photoprism.features.ext.memories.data.storage.MemoriesRepository
import ua.com.radiokot.photoprism.features.ext.memories.logic.UpdateMemoriesUseCase

/**
 * This is only for demo purposes.
 */
class MemoriesDemoActivity : BaseActivity() {
    private lateinit var view: ActivityMemoriesDemoBinding

    private val repository: MemoriesRepository by inject()
    private val memoriesNotificationsManager: MemoriesNotificationsManager by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        view = ActivityMemoriesDemoBinding.inflate(layoutInflater)
        setContentView(view.root)

        setSupportActionBar(view.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        view.clearButton.setThrottleOnClickListener {
            repository.clear()
                .subscribeBy()
                .autoDispose(this)
        }

        view.updateNowButton.setThrottleOnClickListener {
            get<UpdateMemoriesUseCase>()
                .invoke()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe {
                    Toast.makeText(
                        this,
                        getString(R.string.loading_data_progress),
                        Toast.LENGTH_SHORT
                    ).show()
                }
                .subscribeBy { foundMemories ->
                    if (foundMemories.isEmpty()) {
                        Toast.makeText(this, "Nothing found", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "Found ${foundMemories.size}", Toast.LENGTH_SHORT)
                            .show()
                    }
                }
                .autoDispose(this)
        }

        view.unseeAllButton.setThrottleOnClickListener {
            repository.markAllAsNotSeenLocally()
        }

        view.notifyButton.setThrottleOnClickListener {
            memoriesNotificationsManager
                .notifyNewMemories(
                    bigPictureUrl = "https://i.photoprism.app/prism?cover=64&amp;style=centered%20dark&amp;title=Hi%20there",
                )
                .subscribeBy()
                .autoDispose(this)
        }

        repository
            .items
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy { memories ->
                memories.maxByOrNull(Memory::createdAt)?.let { mostRecentMemory ->
                    view.statusTextView.text =
                        "Most recent: ${mostRecentMemory.createdAt}"
                }
            }
            .autoDispose(this)
    }
}
