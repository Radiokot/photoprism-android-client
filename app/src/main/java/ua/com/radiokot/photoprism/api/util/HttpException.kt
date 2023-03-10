package ua.com.radiokot.photoprism.api.util

import okhttp3.Response
import okhttp3.ResponseBody
import okio.IOException

class HttpException(
    val code: Int,
    val httpMessage: String,
    val body: ResponseBody?
) : IOException("$code $httpMessage") {
    constructor(response: Response) : this(
        code = response.code,
        httpMessage = response.message,
        body = response.body
    )

    constructor(response: retrofit2.Response<*>) : this(
        code = response.code(),
        httpMessage = response.message(),
        body = response.errorBody()
    )
}