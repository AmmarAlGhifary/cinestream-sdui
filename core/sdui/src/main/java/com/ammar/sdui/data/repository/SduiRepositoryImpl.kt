package com.ammar.sdui.data.repository

import com.ammar.network.source.FirebaseDataSource
import com.ammar.sdui.domain.model.SduiScreen
import com.ammar.sdui.domain.repository.SduiRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import javax.inject.Inject

class SduiRepositoryImpl @Inject constructor(
    private val firebaseDataSource: FirebaseDataSource,
    private val json: Json
) : SduiRepository {
    override fun getScreen(screenId: String): Flow<Result<SduiScreen>> {
        return firebaseDataSource.getScreenBlueprint(screenId).map { result ->
            result.fold(
                onSuccess = { jsonString ->
                    try {
                        // 3. The Magic: Decode the raw string into our polymorphic models!
                        // Because SduiScreen contains SduiComponent children, the parser walks
                        // the whole tree automatically based on the "type" fields.
                        val screen = json.decodeFromString<SduiScreen>(jsonString)
                        Result.success(screen)
                    } catch (e: Exception) {
                        // Catch parsing errors (e.g., if the JSON is malformed or missing a type)
                        Result.failure(Exception("Failed to parse SDUI JSON: ${e.message}", e))
                    }
                },
                onFailure = { error ->
                    Result.failure(error)
                })
        }
    }
}