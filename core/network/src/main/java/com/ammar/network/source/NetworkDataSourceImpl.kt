package com.ammar.network.source

import android.util.Log
import com.ammar.network.error.AppError
import com.ammar.network.error.HttpErrorParser
import com.ammar.network.error.toAppError
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

class NetworkDataSourceImpl @Inject constructor(
    private val apiService: SduiApiService
) : NetworkDataSource {

    /** Emits once. A failure is always an [AppError]. */
    override fun getScreenBlueprint(screenId: String, queryParams: Map<String, String>): Flow<Result<String>> = flow {
        emit(fetch(screenId, queryParams))
    }

    private suspend fun fetch(screenId: String, queryParams: Map<String, String>): Result<String> =
        try {
            val response = apiService.getScreenBlueprint(screenId, queryParams)
            val body = response.body()
            when {
                !response.isSuccessful -> Result.failure(HttpErrorParser.parse(response).also(::log))
                body == null -> Result.failure(AppError.Parse())
                else -> Result.success(body.string())
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e.toAppError())
        }

    /** Server-written titles and messages stay out of the log. */
    private fun log(error: AppError.Http) {
        Log.w(TAG, "HTTP ${error.status} code=${error.serverCode} request_id=${error.requestId}")
    }

    private companion object {
        const val TAG = "CineStreamNetwork"
    }
}
