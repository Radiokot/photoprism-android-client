package ua.com.radiokot.photoprism.api.util

import retrofit2.Call
import retrofit2.CallAdapter
import retrofit2.Response
import retrofit2.Retrofit
import ua.com.radiokot.photoprism.extension.checkNotNull
import java.io.IOException
import java.io.InterruptedIOException
import java.lang.reflect.Type
import java.net.HttpURLConnection

class SyncCallAdapter<T : Any>(
    private val returnType: Type,
) : CallAdapter<T, T> {
    override fun responseType(): Type =
        returnType

    @kotlin.jvm.Throws(IOException::class)
    override fun adapt(call: Call<T>): T {
        val response: Response<T>

        try {
            response = call.execute()
        } catch (interruption: InterruptedIOException) {
            // Call cancellation is important for OkHTTP.
            call.cancel()
            throw interruption
        }

        if (response.code() >= HttpURLConnection.HTTP_BAD_REQUEST) {
            throw retrofit2.HttpException(response)
        }

        return if (returnType == Unit::class.java) {
            // Expected.
            @Suppress("UNCHECKED_CAST")
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