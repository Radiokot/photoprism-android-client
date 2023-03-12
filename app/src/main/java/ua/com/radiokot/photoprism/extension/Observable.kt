package ua.com.radiokot.photoprism.extension

import androidx.lifecycle.LiveData
import io.reactivex.rxjava3.core.Observable

fun <T: Observable<R>, R> T.toLiveData() = object : LiveData<R>() {

}