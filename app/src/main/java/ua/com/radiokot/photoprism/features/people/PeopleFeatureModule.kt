package ua.com.radiokot.photoprism.features.people

import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.module.dsl.scopedOf
import org.koin.dsl.module
import ua.com.radiokot.photoprism.env.data.model.EnvSession
import ua.com.radiokot.photoprism.features.gallery.search.logic.SearchPredicates
import ua.com.radiokot.photoprism.features.people.data.model.Person
import ua.com.radiokot.photoprism.features.people.data.storage.PeopleRepository
import ua.com.radiokot.photoprism.features.people.view.model.PeopleSelectionViewModel

val peopleFeatureModule = module {

    scope<EnvSession> {
        scopedOf(::PeopleRepository)


        viewModel {
            PeopleSelectionViewModel(
                peopleRepository = get(),
                searchPredicate = { person: Person, query: String ->
                    SearchPredicates.generalCondition(query, person.name)
                },
                previewUrlFactory = get(),
            )
        }
    }
}
