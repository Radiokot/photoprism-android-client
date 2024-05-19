package ua.com.radiokot.photoprism.features.ext.store.api

import retrofit2.http.GET
import retrofit2.http.Headers
import ua.com.radiokot.photoprism.features.ext.store.api.model.FeaturesOnSaleResponse
import java.io.IOException

interface FeatureStoreService {
    @kotlin.jvm.Throws(IOException::class)
    @Headers("Accept: application/json")
    @GET("v1/features")
    fun getFeaturesOnSale(): FeaturesOnSaleResponse
}
