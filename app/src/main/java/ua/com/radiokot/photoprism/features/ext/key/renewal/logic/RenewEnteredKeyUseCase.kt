package ua.com.radiokot.photoprism.features.ext.key.renewal.logic

import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import retrofit2.HttpException
import ua.com.radiokot.photoprism.extension.kLogger
import ua.com.radiokot.photoprism.extension.toSingle
import ua.com.radiokot.photoprism.features.ext.api.OfflineLicenseKeyService
import ua.com.radiokot.photoprism.features.ext.api.model.KeyRenewalRequest
import ua.com.radiokot.photoprism.features.ext.key.logic.HardwareIdentifier
import ua.com.radiokot.photoprism.features.ext.key.renewal.logic.RenewEnteredKeyUseCase.RenewalNotAvailableException
import java.net.HttpURLConnection

/**
 * Requests a copy of the entered key which can be used on this device.
 *
 * @see [RenewalNotAvailableException]
 */
class RenewEnteredKeyUseCase(
    private val issuerId: String,
    private val offlineLicenseKeyService: OfflineLicenseKeyService,
    private val hardwareIdentifier: HardwareIdentifier,
) {
    private val log = kLogger("RenewEnteredKeyUseCase")

    operator fun invoke(
        key: String,
    ): Single<String> =
        getRenewedKey(
            key = key,
            hardware = hardwareIdentifier.getHardwareIdentifier(),
        )
            .onErrorResumeNext { error ->
                if (error is HttpException && error.code() == HttpURLConnection.HTTP_FORBIDDEN) {
                    log.debug {
                        "invoke(): renewal_not_available:" +
                                "\nkey=$key"
                    }

                    Single.error(RenewalNotAvailableException())
                } else {
                    Single.error(error)
                }
            }

    private fun getRenewedKey(
        key: String,
        hardware: String,
    ): Single<String> = {
        log.debug {
            "getRenewedKey(): requesting_renewal:" +
                    "\nkey=$key," +
                    "\nhardware=$hardware," +
                    "\nissuerId=$issuerId"
        }

        offlineLicenseKeyService
            .renewKey(
                issuerId = issuerId,
                request = KeyRenewalRequest(
                    key = key,
                    hardware = hardware,
                )
            )
            .data
            .attributes
            .key
    }.toSingle().subscribeOn(Schedulers.io())

    /**
     * Renewal is not available as already used recently.
     */
    class RenewalNotAvailableException : Exception()
}
