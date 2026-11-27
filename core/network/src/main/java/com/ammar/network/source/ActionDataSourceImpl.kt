package com.ammar.network.source

import com.ammar.network.error.AppError
import com.ammar.network.error.HttpErrorParser
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.json.JsonObject
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException
import kotlin.coroutines.resume

internal class ActionDataSourceImpl(
    private val client: OkHttpClient,
    private val baseUrl: HttpUrl
) : ActionDataSource {

    override suspend fun send(method: String, endpoint: String, body: JsonObject?): Result<String> = apiResult {
        val verb = method.uppercase()
        // The endpoint comes from the server. It may only name a path on our own backend.
        val url = baseUrl.resolve(endpoint)?.takeIf { it.isSameOrigin(baseUrl) }
        if (url == null || verb !in METHODS) {
            Result.failure(AppError.InvalidRequest())
        } else {
            val payload = if (verb == "GET") null else body?.toString().orEmpty().toRequestBody(JSON)
            execute(Request.Builder().url(url).method(verb, payload).build())
        }
    }

    private suspend fun execute(request: Request): Result<String> = suspendCancellableCoroutine { continuation ->
        val call = client.newCall(request)
        continuation.invokeOnCancellation { call.cancel() }
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                continuation.resume(Result.failure(e))
            }

            override fun onResponse(call: Call, response: Response) {
                val result = runCatching {
                    response.use {
                        if (it.isSuccessful) Result.success(it.body?.string().orEmpty())
                        else Result.failure(
                            HttpErrorParser.parse(it.code, it.headers, it.body).also(::logHttpError)
                        )
                    }
                }.getOrElse { Result.failure(it) }
                continuation.resume(result)
            }
        })
    }

    private companion object {
        val JSON = "application/json".toMediaType()
        val METHODS = setOf("GET", "POST", "PUT", "PATCH", "DELETE")
    }
}
