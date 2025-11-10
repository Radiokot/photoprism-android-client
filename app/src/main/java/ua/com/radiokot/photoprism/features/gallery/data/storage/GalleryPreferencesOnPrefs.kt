package ua.com.radiokot.photoprism.features.gallery.data.storage

import android.content.SharedPreferences
import com.google.common.hash.Hashing
import io.reactivex.rxjava3.subjects.BehaviorSubject
import ua.com.radiokot.photoprism.extension.kLogger
import ua.com.radiokot.photoprism.extension.tryOrNull
import ua.com.radiokot.photoprism.features.gallery.data.model.RawSharingMode
import ua.com.radiokot.photoprism.features.gallery.view.model.GalleryItemScale
import ua.com.radiokot.photoprism.features.gallery.data.model.GalleryItemsOrder
import ua.com.radiokot.photoprism.util.booleanPreferenceSubject
import ua.com.radiokot.photoprism.util.stringifyPreferenceSubject
import ua.com.radiokot.photoprism.util.intPreferenceSubject

class GalleryPreferencesOnPrefs(
    private val preferences: SharedPreferences,
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

    override val livePhotosAsImages: BehaviorSubject<Boolean> =
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

    override val rawSharingMode: BehaviorSubject<RawSharingMode> =
        stringifyPreferenceSubject(
            preferences = preferences,
            key = "${keyPrefix}_raw_sharing",
            defaultValue = RawSharingMode.COMPATIBLE_JPEG,
            stringSerializer = RawSharingMode::name,
            stringDeserializer = RawSharingMode::valueOf,
        )

    override val cacheSizeLimitInMb: BehaviorSubject<Int> =
        intPreferenceSubject(
            preferences = preferences,
            key = "${keyPrefix}_cache_size_in_mb",
            defaultValue = 500,
            onValuePut = { _, newValue ->
                log.debug {
                    "cacheSizeLimitInMb::onNext(): set_value:" +
                            "\nvalue=$newValue"
                }
            }

        )

    override fun getItemsOrderBySearchQuery(
        searchQuery: String?,
    ): BehaviorSubject<GalleryItemsOrder> {

        val keyPrefix =
            if (searchQuery != null)
                Hashing
                    .sha256()
                    .hashString(searchQuery, Charsets.UTF_8)
                    .toString()
            else
                "default"


        return stringifyPreferenceSubject(
            preferences = preferences,
            key = "${keyPrefix}_items_order",
            defaultValue = GalleryItemsOrder.NEWEST_FIRST,
            stringDeserializer = { valueString ->
                tryOrNull {
                    GalleryItemsOrder.valueOf(valueString)
                }
            },
            stringSerializer = { newValue ->
                newValue.name.also {
                    log.debug {
                        "itemsOrder::onNext(): set_value:" +
                                "\nsearchQuery=$searchQuery," +
                                "\nkeyPrefix=$keyPrefix," +
                                "\nvalue=$newValue"
                    }
                }
            },
        )
    }
}
