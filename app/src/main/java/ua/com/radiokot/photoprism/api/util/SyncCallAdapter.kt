package ua.com.radiokot.photoprism.api.util

import retrofit2.Call
import retrofit2.CallAdapter
import retrofit2.Retrofit
import ua.com.radiokot.photoprism.extension.checkNotNull
import java.lang.reflect.Type
import java.net.HttpURLConnection

class SyncCallAdapter<T : Any>(
    private val returnType: Type,
) : CallAdapter<T, T> {
    override fun responseType(): Type =
        returnType

    override fun adapt(call: Call<T>): T {
        val response = call.execute()

        if (response.code() >= HttpURLConnection.HTTP_BAD_REQUEST) {
            throw HttpException(response)
        }

        return if (returnType == Unit::class.java) {
            Unit as T
        } else {
            response.body().checkNotNull()
        }
    }

    companion object Factory : CallAdapter.Factory() {
        override fun get(
            returnType: Type,
            annotations: Array<out Annotation>,
            retrofit: Retrofit
        ): CallAdapter<*, *> = SyncCallAdapter<Any>(returnType)
    }
}