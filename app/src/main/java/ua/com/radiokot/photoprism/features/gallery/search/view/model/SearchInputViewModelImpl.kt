package ua.com.radiokot.photoprism.features.gallery.search.view.model

import androidx.lifecycle.MutableLiveData
import io.reactivex.rxjava3.core.Observable
import java.util.concurrent.TimeUnit

class SearchInputViewModelImpl(
    private val debounceTimeoutMs: Long = 400,
): SearchInputViewModel {
    override val rawSearchInput: MutableLiveData<String> = MutableLiveData("")

    override val searchInputObservable: Observable<String>
        get() = Observable
            .create { emitter ->
                rawSearchInput.observeForever(emitter::onNext)
            }
            .distinctUntilChanged()
            .debounce { value ->
                // Apply debounce to the input unless it is empty (input is cleared).
                if (value.isEmpty())
                    Observable.just(0L)
                else
                    Observable.timer(debounceTimeoutMs, TimeUnit.MILLISECONDS)
            }

    override val currentSearchInput: String?
        get() = rawSearchInput.value?.takeIf(String::isNotEmpty)
}
