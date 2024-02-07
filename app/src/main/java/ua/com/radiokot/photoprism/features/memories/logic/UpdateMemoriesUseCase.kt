package ua.com.radiokot.photoprism.features.memories.logic

import io.reactivex.rxjava3.core.Completable
import ua.com.radiokot.photoprism.extension.kLogger
import ua.com.radiokot.photoprism.features.memories.data.storage.MemoriesRepository

class UpdateMemoriesUseCase(
    private val getMemoriesUseCase: GetMemoriesUseCase,
    private val memoriesRepository: MemoriesRepository,
) {
    private val log = kLogger("UpdateMemoriesUseCase")

    operator fun invoke(): Completable =
        getMemoriesUseCase
            .invoke()
            .doOnSubscribe {
                log.debug {
                    "invoke(): getting_memories"
                }
            }
            .doOnSuccess {
                log.debug {
                    "invoke(): got_memories:" +
                            "\nmemories=${it.size}"
                }
            }
            .flatMapCompletable(memoriesRepository::add)
            .doOnComplete {
                log.debug {
                    "invoke(): added_to_repository"
                }
            }
}
