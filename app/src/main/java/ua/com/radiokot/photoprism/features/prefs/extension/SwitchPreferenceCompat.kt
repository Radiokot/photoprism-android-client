package ua.com.radiokot.photoprism.features.prefs.extension

import androidx.lifecycle.LifecycleOwner
import androidx.preference.SwitchPreferenceCompat
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.subjects.BehaviorSubject
import ua.com.radiokot.photoprism.extension.autoDispose

fun SwitchPreferenceCompat.bindToSubject(
    subject: BehaviorSubject<Boolean>,
    lifecycleOwner: LifecycleOwner,
) {
    subject
        .observeOn(AndroidSchedulers.mainThread())
        .subscribeBy(onNext = this::setChecked)
        // View lifecycle owner is not available at the init time.
        .autoDispose(lifecycleOwner)

    setOnPreferenceChangeListener { _, newValue ->
        subject.onNext(newValue == true)
        true
    }
}
