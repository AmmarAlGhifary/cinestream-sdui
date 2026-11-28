package com.ammar.network.error

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import okhttp3.Headers
import okhttp3.ResponseBody
import retrofit2.Response

/**
 * Reads the backend's error contract:
 * `{"error": {"code", "title", "message", "retryable", "request_id"}}`, plus the Retry-After header.
 *
 * Every field is optional. A missing, oversized, non-JSON or differently shaped body (including the
 * legacy `{"error": "<text>"}`) yields an error that carries only the status.
 */
internal object HttpErrorParser {

    private const val MAX_BODY_BYTES = 64 * 1024L
    private const val MAX_RETRY_AFTER_SECONDS = 24 * 60 * 60

    /** Codes and request ids are logged and shown in debug builds, so only plain tokens are kept. */
    private val TOKEN = Regex("[A-Za-z0-9_.:-]{1,128}")

    fun parse(response: Response<*>): AppError.Http =
        parse(response.code(), response.headers(), response.errorBody())

    fun parse(status: Int, headers: Headers, body: ResponseBody?): AppError.Http {
        val error = body?.let { readErrorObject(it) }
        return AppError.Http(
            status = status,
            serverCode = error?.string("code")?.takeIf { TOKEN.matches(it) },
            serverTitle = error?.string("title"),
            serverMessage = error?.string("message"),
            retryable = (error?.get("retryable") as? JsonPrimitive)
                ?.takeUnless { it.isString }
                ?.booleanOrNull,
            requestId = error?.string("request_id")?.takeIf { TOKEN.matches(it) },
            retryAfterSeconds = parseRetryAfter(headers["Retry-After"])
        )
    }

    /** Only the delay-in-seconds form is supported; an HTTP date or anything else is ignored. */
    fun parseRetryAfter(header: String?): Int? =
        header?.trim()?.toIntOrNull()?.takeIf { it in 1..MAX_RETRY_AFTER_SECONDS }

    // runCatching on purpose: this is synchronous, and even a stack overflow must fall back silently.
    private fun readErrorObject(body: ResponseBody): JsonObject? = runCatching {
        // A chunked response reports no length, so the limit looks at the bytes themselves.
        if (body.source().request(MAX_BODY_BYTES + 1)) return null
        (Json.parseToJsonElement(body.string()) as? JsonObject)?.get("error") as? JsonObject
    }.getOrNull()

    private fun JsonObject.string(key: String): String? =
        (get(key) as? JsonPrimitive)?.takeIf { it.isString }?.content?.trim()?.takeIf { it.isNotEmpty() }
}
