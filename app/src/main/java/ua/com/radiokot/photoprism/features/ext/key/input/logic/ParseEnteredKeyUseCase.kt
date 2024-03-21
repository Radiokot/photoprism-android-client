package ua.com.radiokot.photoprism.features.ext.key.input.logic

import io.reactivex.rxjava3.core.Single

class ParseEnteredKeyUseCase(
    private val keyInput: String,
) {
    operator fun invoke(): Single<Result> {
        return Single.just(Result.Success(keyInput))
//        return when ((0..4).random()) {
//            0 -> Result.Failure.INVALID_FORMAT
//            1 -> Result.Failure.DEVICE_MISMATCH
//            2 -> Result.Failure.EMAIL_MISMATCH
//            3 -> Result.Failure.EXPIRED
//            else -> Result.Success(Unit)
//        }.let { Single.just(it) }
    }

    sealed interface Result {
        data class Success(
            val parsed: Any,
        ) : Result

        enum class Failure : Result {
            INVALID_FORMAT,
            DEVICE_MISMATCH,
            EMAIL_MISMATCH,
            EXPIRED,
            ;
        }
    }

    class Factory() {
        fun get(
            keyInput: String,
        ) = ParseEnteredKeyUseCase(
            keyInput = keyInput,
        )
    }
}
