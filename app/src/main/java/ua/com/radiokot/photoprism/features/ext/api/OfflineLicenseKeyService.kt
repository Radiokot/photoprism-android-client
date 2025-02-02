package ua.com.radiokot.photoprism.features.ext.api

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Path
import ua.com.radiokot.photoprism.features.ext.api.model.FeaturesOnSaleResponse
import ua.com.radiokot.photoprism.features.ext.api.model.KeyRenewalRequest
import ua.com.radiokot.photoprism.features.ext.api.model.KeyRenewalResponse
import java.io.IOException

interface OfflineLicenseKeyService {
    @kotlin.jvm.Throws(IOException::class)
    @Headers("Accept: application/json")
    @GET("v1/features")
    fun getFeaturesOnSale(): FeaturesOnSaleResponse

    @kotlin.jvm.Throws(IOException::class)
    @Headers("Accept: application/json")
    @POST("v1/issuers/{issuerId}/renewal")
    fun renewKey(
        @Path("issuerId")
        issuerId: String,
        @Body
        request: KeyRenewalRequest,
    ): KeyRenewalResponse
}
