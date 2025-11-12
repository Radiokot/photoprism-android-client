package ua.com.radiokot.photoprism.features.labels.data.storage

import io.reactivex.rxjava3.core.Single
import ua.com.radiokot.photoprism.api.labels.service.PhotoPrismLabelsService
import ua.com.radiokot.photoprism.base.data.model.DataPage
import ua.com.radiokot.photoprism.base.data.storage.SimpleCollectionRepository
import ua.com.radiokot.photoprism.extension.toSingle
import ua.com.radiokot.photoprism.features.labels.data.model.Label
import ua.com.radiokot.photoprism.util.PagedCollectionLoader

class LabelsRepository(
    private val isAllLabels: Boolean,
    private val photoPrismLabelsService: PhotoPrismLabelsService,
) : SimpleCollectionRepository<Label>() {

    override fun getCollection(): Single<List<Label>> {
        val loader = PagedCollectionLoader(
            pageProvider = { cursor ->
                {
                    val offset = cursor?.toInt() ?: 0

                    val items = photoPrismLabelsService.getLabels(
                        count = PAGE_LIMIT,
                        offset = offset,
                        all = isAllLabels,
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
            .map { photoPrismLabels ->
                photoPrismLabels.map(::Label)
            }
    }

    class Factory(
        private val photoPrismLabelsService: PhotoPrismLabelsService,
    ) {
        private val cache: MutableMap<Boolean, LabelsRepository> = mutableMapOf()

        fun get(
            isAllLabels: Boolean,
        ) = cache.getOrPut(isAllLabels) {
            LabelsRepository(
                isAllLabels = isAllLabels,
                photoPrismLabelsService = photoPrismLabelsService,
            )
        }
    }

    private companion object {
        private const val PAGE_LIMIT = 120
    }
}
