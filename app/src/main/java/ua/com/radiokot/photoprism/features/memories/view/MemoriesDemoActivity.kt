package ua.com.radiokot.photoprism.features.memories.view

import android.os.Bundle
import android.widget.Toast
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.schedulers.Schedulers
import org.koin.android.ext.android.get
import org.koin.android.ext.android.inject
import ua.com.radiokot.photoprism.base.view.BaseActivity
import ua.com.radiokot.photoprism.databinding.ActivityMemoriesDemoBinding
import ua.com.radiokot.photoprism.extension.autoDispose
import ua.com.radiokot.photoprism.extension.setThrottleOnClickListener
import ua.com.radiokot.photoprism.features.memories.data.model.Memory
import ua.com.radiokot.photoprism.features.memories.data.storage.MemoriesRepository
import ua.com.radiokot.photoprism.features.memories.logic.UpdateMemoriesUseCase

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
                .subscribeBy { gotAnyMemories ->
                    if (!gotAnyMemories) {
                        Toast.makeText(this, "Nothing found", Toast.LENGTH_SHORT).show()
                    }
                }
                .autoDispose(this)
        }

        view.unseeAllButton.setThrottleOnClickListener {
            repository.markAllAsNotSeenLocally()
        }

        view.notifyButton.setThrottleOnClickListener {
            memoriesNotificationsManager.notifyNewMemories()
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
