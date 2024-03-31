package ua.com.radiokot.photoprism.features.ext.key.input.logic

import com.fasterxml.jackson.core.Base64Variants
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import ua.com.radiokot.license.OfflineLicenseKey
import ua.com.radiokot.license.OfflineLicenseKeyVerificationException
import ua.com.radiokot.license.OfflineLicenseKeys
import ua.com.radiokot.photoprism.extension.kLogger
import ua.com.radiokot.photoprism.extension.toSingle
import ua.com.radiokot.photoprism.features.ext.data.storage.GalleryExtensionsStateRepository
import ua.com.radiokot.photoprism.features.ext.key.input.data.model.ParsedKey
import ua.com.radiokot.photoprism.features.ext.key.logic.HardwareIdentifier
import java.security.KeyFactory
import java.security.interfaces.RSAPublicKey
import java.security.spec.X509EncodedKeySpec

class ParseEnteredKeyUseCase(
    private val keyInput: String,
    private val primarySubject: String?,
    private val hardware: String?,
) {
    private val log = kLogger("ParseEnteredKeyUseCase")

    operator fun invoke(): Single<Result> {
        return getIssuerPublicKey()
            .flatMap(::readAndVerifyKey)
            .map { verifiedKey ->
                val parsedKey = ParsedKey(verifiedKey)
                if (parsedKey.extensions.isEmpty()) {
                    Result.Failure.INVALID.also { outcome ->
                        log.warn {
                            "invoke(): parsed_key_has_no_known_extensions:" +
                                    "\nkeyInput=$keyInput," +
                                    "\nverifiedKeyFeatures=${verifiedKey.features}," +
                                    "\noutcome=$outcome"
                        }
                    }
                } else {
                    Result.Success(parsedKey)
                }
            }
            .onErrorResumeNext { error ->
                val verificationError = error as? OfflineLicenseKeyVerificationException
                if (verificationError == null) {
                    log.error(error) {
                        "invoke(): unexpected_parsing_error:" +
                                "\nkeyInput=$keyInput"
                    }

                    Single.error(error)
                } else {
                    val outcome: Result.Failure = when (verificationError) {
                        is OfflineLicenseKeyVerificationException.InvalidFormat,
                        is OfflineLicenseKeyVerificationException.AlgorithmMismatch,
                        is OfflineLicenseKeyVerificationException.InvalidSignature,
                        is OfflineLicenseKeyVerificationException.IssuerMismatch ->
                            Result.Failure.INVALID

                        is OfflineLicenseKeyVerificationException.Expired ->
                            Result.Failure.EXPIRED

                        is OfflineLicenseKeyVerificationException.HardwareMismatch ->
                            Result.Failure.DEVICE_MISMATCH

                        is OfflineLicenseKeyVerificationException.SubjectMismatch ->
                            Result.Failure.EMAIL_MISMATCH
                    }

                    log.debug {
                        "invoke(): verification_failed:" +
                                "\nkeyInput=$keyInput," +
                                "\nprimarySubject=$primarySubject," +
                                "\nerrorMessage='${verificationError.message}'," +
                                "\noutcome=${outcome}"
                    }

                    Single.just(outcome)
                }
            }
    }

    private fun getIssuerPublicKey(): Single<RSAPublicKey> = {
        KeyFactory.getInstance("RSA")
            .generatePublic(
                X509EncodedKeySpec(
                    Base64Variants.PEM.decode(ISSUER_PUB.trimIndent())
                )
            ) as RSAPublicKey
    }.toSingle().subscribeOn(Schedulers.io())

    private fun readAndVerifyKey(issuerPublicKey: RSAPublicKey): Single<OfflineLicenseKey> = {
        log.debug {
            "readAndVerifyKey(): reading_the_key:" +
                    "\nkeyInput=$keyInput," +
                    "\nprimarySubject=$primarySubject," +
                    "\nhardware=$hardware"
        }

        OfflineLicenseKeys.jwt.verifyingReader(
            issuerPublicKey = issuerPublicKey,
            issuer = ISSUER,
            subject = primarySubject,
            hardware = hardware,
        )
            .read(keyInput)
    }.toSingle().subscribeOn(Schedulers.io())

    sealed interface Result {
        data class Success(
            val parsed: ParsedKey,
        ) : Result

        enum class Failure : Result {
            INVALID,
            DEVICE_MISMATCH,
            EMAIL_MISMATCH,
            EXPIRED,
            ;
        }
    }

    class Factory(
        private val extensionsStateRepository: GalleryExtensionsStateRepository,
        private val hardwareIdentifier: HardwareIdentifier?,
    ) {
        fun get(
            keyInput: String,
        ) = ParseEnteredKeyUseCase(
            keyInput = keyInput,
            primarySubject = extensionsStateRepository.currentState.primarySubject,
            hardware = hardwareIdentifier?.getHardwareIdentifier(),
        )
    }

    private companion object {
        // Dearest gentle explorer.
        //
        // I would find it utterly discourteous
        // should there be any alterations to the following constants.
        //
        // Sincerely,
        // O.K.
        private const val ISSUER = "pp-license.radiokot.com.ua"
        private const val ISSUER_PUB = """
            MIIBHzANBgkqhkiG9w0BAQEFAAOCAQwAMIIBBwKB/y6ZK6lHmMfS0T5fA5WsBWpR
            v4gpdtTUbeubzJD2YbEZOAMQWdum97GacQXWhMz3U4lJFoXeRbbSbcXVNWILQK5Q
            whBWepcvZZoEGBj488kchsZ4NLD6Cu8ZUvfZSzJwtlpiZSQdEZzaIeBR/HHMhW7I
            lSB/dClTP1a8UlWCZFU6QnJPZAhbTVFBuC6XJfAjT5rcd0vMoQ3ZYU/5KmzLzFzP
            bhCHjM5c69yuAsvBorzgLKvfwRpR6lJsf8wU7qxOucUcaFSA+3yCa2qBwffOO1xT
            zyrgfTu+7oWVIwmgQiK4ikNPzJKyKn7WZkcze3QEs0qXv8WyhEjEc+aquDExlwID
            AQAB
        """
    }
}
