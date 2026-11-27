package com.ammar.network.source

import com.ammar.network.error.AppError
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class NetworkDataSourceImpl @Inject constructor(
    private val apiService: SduiApiService
) : NetworkDataSource {

    /** Emits once. A failure is always an [AppError]. */
    override fun getScreenBlueprint(screenId: String, queryParams: Map<String, String>): Flow<Result<String>> = flow {
        val result = apiResult {
            // A screen always has a body, so an empty one cannot be read.
            apiService.getScreenBlueprint(screenId, queryParams).bodyOrError()
                .mapCatching { it.ifEmpty { throw AppError.Parse() } }
        }
        emit(result)
    }
}
