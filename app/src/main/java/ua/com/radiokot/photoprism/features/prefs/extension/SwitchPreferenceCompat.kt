package ua.com.radiokot.photoprism.features.prefs.extension

import androidx.lifecycle.LifecycleOwner
import androidx.preference.SwitchPreferenceCompat
import io.reactivex.rxjava3.subjects.BehaviorSubject
import ua.com.radiokot.photoprism.extension.observeOnMain
import ua.com.radiokot.photoprism.extension.subscribe

fun SwitchPreferenceCompat.bindToSubject(
    subject: BehaviorSubject<Boolean>,
    lifecycleOwner: LifecycleOwner,
) {
    subject
        .observeOnMain()
        // View lifecycle owner is not available at the init time.
        .subscribe(lifecycleOwner, ::setChecked)

    setOnPreferenceChangeListener { _, newValue ->
        subject.onNext(newValue == true)
        true
    }
}
