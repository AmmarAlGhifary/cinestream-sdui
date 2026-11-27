package com.ammar.network.source

import android.util.Log
import com.ammar.network.error.AppError
import com.ammar.network.error.HttpErrorParser
import com.ammar.network.error.toAppError
import okhttp3.ResponseBody
import retrofit2.Response
import kotlin.coroutines.cancellation.CancellationException

private const val TAG = "CineStreamNetwork"

/** Runs a call so that every failure arrives as an [AppError]. Cancellation passes through. */
internal suspend fun <T> apiResult(call: suspend () -> Result<T>): Result<T> =
    try {
        call().recoverCatching { throw it.toAppError() }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e.toAppError())
    }

/** The text of a 2xx answer, empty for one without a body. */
internal fun Response<ResponseBody>.bodyOrError(): Result<String> =
    if (isSuccessful) Result.success(body()?.string().orEmpty())
    else Result.failure(HttpErrorParser.parse(this).also(::logHttpError))

/** Server-written titles and messages stay out of the log. */
internal fun logHttpError(error: AppError.Http) {
    Log.w(TAG, "HTTP ${error.status} code=${error.serverCode} request_id=${error.requestId}")
}
