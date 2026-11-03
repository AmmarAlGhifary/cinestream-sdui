package com.ammar.sdui.data.repository

import com.ammar.network.source.NetworkDataSource
import com.ammar.sdui.domain.repository.SduiRepository
import com.ammar.sdui.presentation.registry.SCAFFOLD_TYPE
import io.github.ammaralghifary.blueprint.Blueprint
import io.github.ammaralghifary.blueprint.model.SduiScreen
import io.github.ammaralghifary.blueprint.model.UnknownComponent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import javax.inject.Inject

class SduiRepositoryImpl @Inject constructor(
    private val networkDataSource: NetworkDataSource,
    private val blueprint: Blueprint
) : SduiRepository {

    override fun getScreen(screenId: String, params: Map<String, String>): Flow<Result<SduiScreen>> =
        networkDataSource.getScreenBlueprint(screenId, params).map { result ->
            result.map { jsonString -> decode(jsonString) }.mapCatching { screen ->
                // Only an unreadable envelope fails the screen; a broken node inside it degrades on its own.
                val body = screen.body
                if (body is UnknownComponent && body.error != null) {
                    throw IllegalStateException("Failed to parse SDUI JSON: ${body.error}")
                }
                screen
            }
        }

    private fun decode(jsonString: String): SduiScreen {
        val root = runCatching { blueprint.json.parseToJsonElement(jsonString) }.getOrNull() as? JsonObject
            ?: return blueprint.decodeScreen(jsonString) // let Blueprint report the unreadable payload
        val body = root["body"] as? JsonObject ?: return blueprint.decodeScreen(root)

        val scaffold = buildMap {
            put("type", JsonPrimitive(SCAFFOLD_TYPE))
            put("body", body)
            (root["toolbar"] as? JsonObject)?.let { put("toolbar", it) }
        }
        return blueprint.decodeScreen(JsonObject(root + ("body" to JsonObject(scaffold))))
    }
}
