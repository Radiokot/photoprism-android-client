package ua.com.radiokot.photoprism.features.gallery.data.storage

import android.content.SharedPreferences
import io.reactivex.rxjava3.subjects.BehaviorSubject
import ua.com.radiokot.photoprism.extension.kLogger
import ua.com.radiokot.photoprism.extension.tryOrNull
import ua.com.radiokot.photoprism.features.gallery.data.model.GalleryItemScale
import ua.com.radiokot.photoprism.util.booleanPreferenceSubject
import ua.com.radiokot.photoprism.util.stringifyPreferenceSubject

class GalleryPreferencesOnPrefs(
    preferences: SharedPreferences,
    keyPrefix: String = "gallery",
) : GalleryPreferences {
    private val log = kLogger("GalleryPreferencesOnPrefs")

    override val itemScale: BehaviorSubject<GalleryItemScale> =
        stringifyPreferenceSubject(
            preferences = preferences,
            key = "${keyPrefix}_item_scale",
            defaultValue = GalleryItemScale.NORMAL,
            stringDeserializer = { valueString ->
                tryOrNull {
                    GalleryItemScale.valueOf(valueString)
                }
            },
            stringSerializer = { newValue ->
                newValue.name.also {
                    log.debug {
                        "itemScale::onNext(): set_value:" +
                                "\nvalue=$newValue"
                    }
                }
            },
        )

    override var livePhotosAsImages: BehaviorSubject<Boolean> =
        booleanPreferenceSubject(
            preferences = preferences,
            key = "${keyPrefix}_lp_as_images",
            defaultValue = false,
            onValuePut = { _, newValue ->
                log.debug {
                    "livePhotosAsImages::onNext(): set_value:" +
                            "\nvalue=$newValue"
                }
            }
        )
}
