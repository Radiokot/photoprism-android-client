package ua.com.radiokot.photoprism.features.gallery.search.people.data.storage

import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import ua.com.radiokot.photoprism.api.faces.service.PhotoPrismFacesService
import ua.com.radiokot.photoprism.api.model.PhotoPrismOrder
import ua.com.radiokot.photoprism.api.subjects.service.PhotoPrismSubjectsService
import ua.com.radiokot.photoprism.base.data.model.DataPage
import ua.com.radiokot.photoprism.base.data.storage.SimpleCollectionRepository
import ua.com.radiokot.photoprism.extension.toSingle
import ua.com.radiokot.photoprism.features.gallery.logic.MediaPreviewUrlFactory
import ua.com.radiokot.photoprism.features.gallery.search.people.data.model.Person
import ua.com.radiokot.photoprism.util.PagedCollectionLoader

/**
 * A repository for people which the gallery content can be filtered by.
 *
 * Combined from person subjects (People Recognized) and unknown faces (People New).
 *
 * First go favorite named people.
 */
class PeopleRepository(
    private val photoPrismSubjectsService: PhotoPrismSubjectsService,
    private val photoPrismFacesService: PhotoPrismFacesService,
    private val previewUrlFactory: MediaPreviewUrlFactory,
) : SimpleCollectionRepository<Person>() {
    private val comparator =
        compareByDescending(Person::isFavorite)
            .thenByDescending(Person::hasName)
            .thenByDescending(Person::photoCount)
            .thenBy(Person::name)

    override fun getCollection(): Single<List<Person>> {
        return Single.zip(
            getFromPersonSubjects(),
            getFromUnknownFaces(),
        ) { list1, list2 ->
            list1.toMutableList().apply {
                addAll(list2)
                sortWith(comparator)
            }
        }
    }

    private fun getFromPersonSubjects(): Single<List<Person>> {
        val loader = PagedCollectionLoader(
            pageProvider = { cursor ->
                {
                    val offset = cursor?.toInt() ?: 0

                    val items = photoPrismSubjectsService.getSubjects(
                        count = PAGE_LIMIT,
                        offset = offset,
                        order = PhotoPrismOrder.FAVORITES,
                        type = "person"
                    )

                    DataPage(
                        items = items,
                        nextCursor = (PAGE_LIMIT + offset).toString(),
                        isLast = items.size < PAGE_LIMIT,
                    )
                }.toSingle()
            }
        )

        return loader
            .loadAll()
            .map { personSubjects ->
                personSubjects.map { personSubject ->
                    Person(
                        personSubject = personSubject,
                        previewUrlFactory = previewUrlFactory,
                    )
                }
            }
            .subscribeOn(Schedulers.io())
    }

    private fun getFromUnknownFaces(): Single<List<Person>> {
        val loader = PagedCollectionLoader(
            pageProvider = { cursor ->
                {
                    val offset = cursor?.toInt() ?: 0

                    val items = photoPrismFacesService.getFaces(
                        count = PAGE_LIMIT,
                        offset = offset,
                        order = PhotoPrismOrder.FAVORITES,
                        markers = true,
                        unknown = true,
                    )

                    DataPage(
                        items = items,
                        nextCursor = (PAGE_LIMIT + offset).toString(),
                        isLast = items.size < PAGE_LIMIT,
                    )
                }.toSingle()
            }
        )

        return loader
            .loadAll()
            .map { unknownFaces ->
                unknownFaces.map { unknownFace ->
                    Person(
                        face = unknownFace,
                        previewUrlFactory = previewUrlFactory,
                    )
                }
            }
    }

    /**
     * @return [Person] found by [uid] in the [itemsList]
     * or null if nothing found.
     */
    fun getLoadedPerson(uid: String): Person? =
        itemsList.find { it.id == uid }

    private companion object {
        private const val PAGE_LIMIT = 30
    }
}
