package ua.com.radiokot.photoprism.features.prefs.extension

import androidx.lifecycle.LifecycleOwner
import androidx.preference.Preference
import androidx.preference.Preference.OnPreferenceChangeListener
import androidx.preference.SwitchPreferenceCompat
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.subjects.BehaviorSubject
import ua.com.radiokot.photoprism.extension.observeOnMain
import ua.com.radiokot.photoprism.extension.subscribe

fun SwitchPreferenceCompat.bindToSubject(
    subject: BehaviorSubject<Boolean>,
    lifecycleOwner: LifecycleOwner,
) {
    val currentChangeListener = onPreferenceChangeListener
    if (currentChangeListener is OnPreferenceChangeListenerWithDisposable) {
        currentChangeListener.disposable?.dispose()
    }

    val newChangeListener = OnPreferenceChangeListenerWithDisposable(
        onChange = subject::onNext,
    )

    onPreferenceChangeListener = newChangeListener

    subject
        .observeOnMain()
        .doOnSubscribe(newChangeListener::disposable::set)
        // View lifecycle owner is not available at the init time.
        .subscribe(lifecycleOwner, ::setChecked)
}

private class OnPreferenceChangeListenerWithDisposable(
    val onChange: (newValue: Boolean) -> Unit,
) : OnPreferenceChangeListener {
    var disposable: Disposable? = null

    override fun onPreferenceChange(preference: Preference, newValue: Any?): Boolean {
        onChange(newValue == true)
        return true
    }
}
