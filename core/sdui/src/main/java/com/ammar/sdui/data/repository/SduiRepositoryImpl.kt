package com.ammar.sdui.data.repository

import com.ammar.network.error.AppError
import com.ammar.network.error.toAppError
import com.ammar.network.source.NetworkDataSource
import com.ammar.sdui.domain.model.SduiScaffold
import com.ammar.sdui.domain.repository.SduiRepository
import io.github.ammaralghifary.blueprint.Blueprint
import io.github.ammaralghifary.blueprint.model.SduiComponent
import io.github.ammaralghifary.blueprint.model.SduiScreen
import io.github.ammaralghifary.blueprint.model.UnknownComponent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import javax.inject.Inject

class SduiRepositoryImpl @Inject constructor(
    private val networkDataSource: NetworkDataSource,
    private val blueprint: Blueprint
) : SduiRepository {

    /** A failure is always an [AppError]. */
    override fun getScreen(screenId: String, params: Map<String, String>): Flow<Result<SduiScreen>> =
        networkDataSource.getScreenBlueprint(screenId, params).map { result ->
            result.fold(
                onSuccess = { jsonString -> decodeOrParseError(jsonString) },
                onFailure = { Result.failure(it.toAppError()) }
            )
        }

    private fun decodeOrParseError(jsonString: String): Result<SduiScreen> =
        try {
            Result.success(decode(jsonString))
        } catch (e: Exception) {
            Result.failure(AppError.Parse(e))
        }

    /**
     * Only an unreadable envelope or an unusable body root (invalid, or a type this app does not know)
     * fails the screen. A broken node further down degrades on its own.
     */
    private fun decode(jsonString: String): SduiScreen {
        // Blueprint parses first: it checks the nesting depth on the raw text, before any parser runs.
        val screen = blueprint.decodeScreen(jsonString)
        val body = screen.body
        if (body is UnknownComponent) {
            throw IllegalStateException("Failed to parse SDUI JSON: ${body.error ?: "unsupported screen body type"}")
        }

        // Safe to parse again, the text passed that check.
        val root = blueprint.json.parseToJsonElement(jsonString).jsonObject
        val toolbar = (root["toolbar"] as? JsonObject)?.let { decodeToolbar(it) }
        return screen.copy(body = SduiScaffold(toolbar = toolbar, body = body))
    }

    /**
     * Blueprint's screen format has no toolbar field, so the toolbar is decoded as the body of its own
     * screen. That keeps it at the nesting depth the server sent it at.
     */
    private fun decodeToolbar(toolbar: JsonObject): SduiComponent =
        blueprint.decodeScreen(JsonObject(mapOf("body" to toolbar))).body
}
