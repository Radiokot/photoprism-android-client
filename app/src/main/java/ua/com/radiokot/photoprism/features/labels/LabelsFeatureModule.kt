package ua.com.radiokot.photoprism.features.labels

import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.bind
import org.koin.dsl.module
import ua.com.radiokot.photoprism.di.retrofitApiModule
import ua.com.radiokot.photoprism.env.data.model.EnvSession
import ua.com.radiokot.photoprism.features.gallery.search.logic.SearchPredicates
import ua.com.radiokot.photoprism.features.labels.data.storage.LabelsRepository
import ua.com.radiokot.photoprism.features.labels.view.model.LabelsViewModel

val labelsFeatureModule = module {
    includes(
        retrofitApiModule,
    )

    scope<EnvSession> {
        scoped {
            LabelsRepository.Factory(
                photoPrismLabelsService = get(),
            )
        } bind LabelsRepository.Factory::class

        viewModel {
            LabelsViewModel(
                labelsRepositoryFactory = get(),
                searchPredicate = { label, query ->
                    SearchPredicates.generalCondition(query, label.name, label.slug)
                },
                previewUrlFactory = get(),
            )
        }
    }
}
