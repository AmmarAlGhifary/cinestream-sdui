package com.ammar.sdui.data.repository

import com.ammar.network.source.ActionDataSource
import com.ammar.sdui.domain.model.ActionResult
import com.ammar.sdui.domain.model.ApiAction
import com.ammar.sdui.domain.model.SduiIconButton
import com.ammar.sdui.domain.repository.ActionRepository
import io.github.ammaralghifary.blueprint.Blueprint
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import javax.inject.Inject

class ActionRepositoryImpl @Inject constructor(
    private val actionDataSource: ActionDataSource,
    private val blueprint: Blueprint
) : ActionRepository {

    override suspend fun send(action: ApiAction): Result<ActionResult> =
        actionDataSource.send(action.method, action.endpoint, action.body).map { read(it) }

    /** The request succeeded, so an unreadable answer is an empty result, not a failure. */
    private fun read(answer: String): ActionResult {
        val root = runCatching { blueprint.json.parseToJsonElement(answer) as? JsonObject }.getOrNull()
            ?: return ActionResult()
        return ActionResult(
            iconButton = (root["icon_button"] as? JsonObject)?.let { decodeIconButton(it) },
            isFavorite = (root["is_favorite"] as? JsonPrimitive)?.takeUnless { it.isString }?.booleanOrNull
        )
    }

    /** Decoded as the body of its own screen. A button that lost its action is not usable. */
    private fun decodeIconButton(button: JsonObject): SduiIconButton? =
        runCatching { blueprint.decodeScreen(JsonObject(mapOf("body" to button))).body }
            .getOrNull()
            .let { it as? SduiIconButton }
            ?.takeIf { it.action is ApiAction }
}
