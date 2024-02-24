package ua.com.radiokot.photoprism.features.memories.view

import android.os.Bundle
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.schedulers.Schedulers
import org.koin.android.ext.android.get
import org.koin.android.ext.android.inject
import ua.com.radiokot.photoprism.base.view.BaseActivity
import ua.com.radiokot.photoprism.databinding.ActivityMemoriesDemoBinding
import ua.com.radiokot.photoprism.extension.autoDispose
import ua.com.radiokot.photoprism.extension.setThrottleOnClickListener
import ua.com.radiokot.photoprism.features.memories.data.storage.MemoriesRepository
import ua.com.radiokot.photoprism.features.memories.logic.UpdateMemoriesUseCase

/**
 * This is only for demo purposes.
 */
class MemoriesDemoActivity : BaseActivity() {
    private lateinit var view: ActivityMemoriesDemoBinding

    private val repository: MemoriesRepository by inject()

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
                .subscribeBy()
                .autoDispose(this)
        }

        view.unseeAllButton.setThrottleOnClickListener {
            repository.markAllAsNotSeenLocally()
        }
    }
}
