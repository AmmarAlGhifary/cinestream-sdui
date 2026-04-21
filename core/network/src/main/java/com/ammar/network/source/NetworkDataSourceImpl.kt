package com.ammar.network.source

import com.ammar.cinestream.core.network.source.SduiApiService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class NetworkDataSourceImpl @Inject constructor(
    private val apiService: SduiApiService
) : NetworkDataSource {

    override fun getScreenBlueprint(screenId: String, params: Map<String, String>): Flow<Result<String>> = flow {
        try {
            val response = apiService.getScreenBlueprint(screenId, params)

            if (response.isSuccessful && response.body() != null) {
                val rawJson = response.body()!!.string()
                emit(Result.success(rawJson))
            } else {
                emit(Result.failure(Exception("Network error: ${response.code()} ${response.message()}")))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }
}