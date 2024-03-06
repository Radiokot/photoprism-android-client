package ua.com.radiokot.photoprism.features.memories.logic

import io.reactivex.rxjava3.core.Single
import ua.com.radiokot.photoprism.extension.kLogger
import ua.com.radiokot.photoprism.features.memories.data.model.Memory
import ua.com.radiokot.photoprism.features.memories.data.storage.MemoriesRepository

class UpdateMemoriesUseCase(
    private val getMemoriesUseCase: GetMemoriesUseCase,
    private val memoriesRepository: MemoriesRepository,
) {
    private val log = kLogger("UpdateMemoriesUseCase")
    private var foundMemories = emptyList<Memory>()

    operator fun invoke(): Single<List<Memory>> =
        getMemoriesUseCase
            .invoke()
            .doOnSubscribe {
                log.debug {
                    "invoke(): getting_memories"
                }
            }
            .doOnSuccess { memories ->
                log.debug {
                    "invoke(): got_memories:" +
                            "\nmemories=${memories.size}"
                }

                foundMemories = memories
            }
            .flatMapCompletable(memoriesRepository::add)
            .doOnComplete {
                log.debug {
                    "invoke(): added_to_repository"
                }
            }
            .toSingle { foundMemories }
}
