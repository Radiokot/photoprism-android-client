package ua.com.radiokot.photoprism.features.ext.key.input.logic

import io.reactivex.rxjava3.core.Single

class ParseEnteredKeyUseCase(
    private val keyInput: String,
) {

    /**
     * @see InvalidFormatException
     * @see DeviceMismatchException
     * @see EmailMismatchException
     * @see ExpiredException
     */
    operator fun invoke(): Single<Any> {
        return when ((0..3).random()) {
            0 -> Single.error(InvalidFormatException())
            1 -> Single.error(DeviceMismatchException())
            2 -> Single.error(EmailMismatchException())
            3 -> Single.error(ExpiredException())
            else -> Single.just(Unit)
        }
    }

    class InvalidFormatException : RuntimeException("The input has an invalid format")
    class DeviceMismatchException : RuntimeException("This key is not issued for this device")
    class EmailMismatchException : RuntimeException("This key is not issued for known email")
    class ExpiredException : RuntimeException("This key is expired")

    class Factory() {
        fun get(
            keyInput: String,
        ) = ParseEnteredKeyUseCase(
            keyInput = keyInput,
        )
    }
}
