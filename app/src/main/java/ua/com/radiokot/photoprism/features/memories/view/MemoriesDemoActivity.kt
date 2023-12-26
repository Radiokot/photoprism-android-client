package ua.com.radiokot.photoprism.features.memories.view

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.schedulers.Schedulers
import org.koin.android.ext.android.get
import ua.com.radiokot.photoprism.base.view.BaseActivity
import ua.com.radiokot.photoprism.databinding.ActivityMemoriesDemoBinding
import ua.com.radiokot.photoprism.extension.autoDispose
import ua.com.radiokot.photoprism.extension.kLogger
import ua.com.radiokot.photoprism.features.gallery.data.storage.SimpleGalleryMediaRepository
import ua.com.radiokot.photoprism.features.memories.logic.GetMemoriesUseCase
import ua.com.radiokot.photoprism.features.viewer.slideshow.view.SlideshowActivity

/**
 * This is only for demo purposes.
 */
class MemoriesDemoActivity : BaseActivity() {
    private val log = kLogger("MemoriesDemoActivity")

    private lateinit var view: ActivityMemoriesDemoBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        view = ActivityMemoriesDemoBinding.inflate(layoutInflater)
        setContentView(view.root)

        setSupportActionBar(view.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        get<GetMemoriesUseCase>()
            .invoke()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSubscribe {
                view.statusTextView.text = "Loading..."
            }
            .doOnError {
                view.statusTextView.text = "Failed loading: $it"
                log.error("failed_loading_memories", it)
            }
            .subscribeBy { memoriesByYear ->
                view.statusTextView.text = "Done"
                memoriesByYear.forEach { (year, searchQuery) ->
                    view.memoriesLayout.addView(
                        Button(view.memoriesLayout.context).apply {
                            text = year.toString()
                            setOnClickListener {
                                startActivity(
                                    Intent(this@MemoriesDemoActivity, SlideshowActivity::class.java)
                                        .putExtras(
                                            SlideshowActivity.getBundle(
                                                mediaIndex = 0,
                                                repositoryParams = SimpleGalleryMediaRepository.Params(
                                                    query = searchQuery,
                                                )
                                            )
                                        )
                                )
                            }
                        }
                    )
                }
            }
            .autoDispose(this)
    }
}
