package com.ammar.sdui.domain.model

import io.github.ammaralghifary.blueprint.model.SduiScreen
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * What a successful action answered. Both parts are optional.
 *
 * @param iconButton the button as it should look now.
 * @param isFavorite the new favorite state.
 */
data class ActionResult(
    val iconButton: SduiIconButton? = null,
    val isFavorite: Boolean? = null
)

/**
 * This screen with the toolbar button that sent [action] brought up to date: replaced by the server's
 * button, or else redrawn from the favorite state. Null when neither is possible, which means the
 * screen has to be loaded again.
 */
fun SduiScreen.updatedBy(action: ApiAction, result: ActionResult): SduiScreen? {
    val scaffold = body as? SduiScaffold ?: return null
    val appBar = scaffold.toolbar as? SduiAppBar ?: return null
    val index = appBar.actions.indexOfFirst {
        ((it as? SduiIconButton)?.action as? ApiAction)?.hasSameTarget(action) == true
    }
    if (index < 0) return null

    val old = appBar.actions[index] as SduiIconButton
    val updated = result.iconButton ?: result.isFavorite?.let { old.showingFavorite(it) } ?: return null
    val actions = appBar.actions.toMutableList().apply { set(index, updated) }
    return copy(body = scaffold.copy(toolbar = appBar.copy(actions = actions)))
}

/** The same request about the same movie, whatever state it asks for. */
private fun ApiAction.hasSameTarget(other: ApiAction): Boolean =
    endpoint == other.endpoint && method == other.method &&
        body?.get("movie_id") == other.body?.get("movie_id")

/** The heart for [isFavorite]. Its action asks for the opposite state, as the contract does. */
private fun SduiIconButton.showingFavorite(isFavorite: Boolean): SduiIconButton? {
    val action = action as? ApiAction ?: return null
    val body = action.body.orEmpty() + ("favorite" to JsonPrimitive(!isFavorite))
    return copy(
        iconName = if (isFavorite) "favorite" else "favorite_border",
        action = action.copy(body = JsonObject(body))
    )
}
