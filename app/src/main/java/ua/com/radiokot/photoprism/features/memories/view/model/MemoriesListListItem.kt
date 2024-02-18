package ua.com.radiokot.photoprism.features.memories.view.model

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.adapters.ItemAdapter
import com.mikepenz.fastadapter.items.AbstractItem
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.kotlin.subscribeBy
import org.koin.core.component.KoinScopeComponent
import org.koin.core.component.inject
import org.koin.core.scope.Scope
import ua.com.radiokot.photoprism.R
import ua.com.radiokot.photoprism.di.DI_SCOPE_SESSION
import ua.com.radiokot.photoprism.features.memories.data.storage.MemoriesRepository

class MemoriesListListItem : AbstractItem<MemoriesListListItem.ViewHolder>() {
    override val layoutRes: Int =
        R.layout.list_item_memories_list

    override val type: Int =
        R.id.memories_recycler_view

    override var identifier: Long =
        "memories".hashCode().toLong()

    override fun getViewHolder(v: View): ViewHolder =
        ViewHolder(v)

    // TODO: PoC solution.
    class ViewHolder(
        itemView: View
    ) : FastAdapter.ViewHolder<MemoriesListListItem>(itemView),
        KoinScopeComponent {
        override val scope: Scope
            get() = getKoin().getScope(DI_SCOPE_SESSION)

        private val memoriesRepository: MemoriesRepository by inject()
        private val memoriesRecyclerView = itemView as RecyclerView
        private val adapter = ItemAdapter<MemoryListItem>()
        private var subscriptionDisposable: Disposable? = null

        init {
            println("OOLEG here")
            memoriesRecyclerView.adapter = FastAdapter.with(adapter)
            memoriesRepository.update()
        }

        override fun bindView(item: MemoriesListListItem, payloads: List<Any>) {
            subscriptionDisposable?.dispose()
            subscriptionDisposable = memoriesRepository
                .items
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy { memories ->
                    adapter.setNewList(memories.map(::MemoryListItem))
                }
        }

        override fun unbindView(item: MemoriesListListItem) {
            subscriptionDisposable?.dispose()
        }
    }
}
