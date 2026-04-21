package com.ammar.sdui.data.repository

import com.ammar.network.source.NetworkDataSource
import com.ammar.sdui.domain.model.SduiScreen
import com.ammar.sdui.domain.repository.SduiRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import javax.inject.Inject

class SduiRepositoryImpl @Inject constructor(
    private val networkDataSource: NetworkDataSource,
    private val json: Json
) : SduiRepository {
    override fun getScreen(screenId: String, params: Map<String, String>): Flow<Result<SduiScreen>> {
        return networkDataSource.getScreenBlueprint(screenId, params).map { result ->
            result.fold(
                onSuccess = { jsonString ->
                    try {
                        val screen = json.decodeFromString<SduiScreen>(jsonString)
                        Result.success(screen)
                    } catch (e: Exception) {
                        Result.failure(Exception("Failed to parse SDUI JSON: ${e.message}", e))
                    }
                },
                onFailure = { error ->
                    Result.failure(error)
                })
        }
    }

}