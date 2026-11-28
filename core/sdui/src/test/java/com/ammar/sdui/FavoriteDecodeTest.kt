package com.ammar.sdui

import com.ammar.sdui.domain.model.ApiAction
import com.ammar.sdui.domain.model.ShareAction
import com.ammar.sdui.domain.model.SduiAppBar
import com.ammar.sdui.domain.model.SduiIconButton
import com.ammar.sdui.presentation.registry.CineStreamRegistry
import io.github.ammaralghifary.blueprint.Blueprint
import io.github.ammaralghifary.blueprint.model.DecodeError
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** The detail toolbar exactly as the backend sends it: the heart's body carries a real boolean. */
class FavoriteDecodeTest {

    private val errors = mutableListOf<DecodeError>()
    private val blueprint = Blueprint(CineStreamRegistry, onDecodeError = { errors += it })

    private fun assertToolbar(fixture: String, icon: String, nextState: Boolean) {
        val json = requireNotNull(javaClass.classLoader?.getResource(fixture)).readText()
        val appBar = blueprint.decodeScreen("""{"screen_id":"movie_detail_screen","body":$json}""").body
            as SduiAppBar

        val (heart, share) = appBar.actions.map { it as SduiIconButton }
        assertEquals(emptyList<DecodeError>(), errors)
        assertEquals("the heart comes first", icon, heart.iconName)
        val action = heart.action as ApiAction
        assertEquals("/api/favorite", action.endpoint)
        assertEquals("POST", action.method)
        val body = action.body!!
        assertEquals(JsonPrimitive("42"), body["movie_id"])
        val favorite = body["favorite"] as JsonPrimitive
        assertFalse("a boolean, not a string", favorite.isString)
        assertEquals(nextState, favorite.boolean)
        assertEquals("""{"movie_id":"42","favorite":$nextState}""", body.toString())
        assertTrue(share.action is ShareAction)
    }

    @Test
    fun `signed out, the empty heart asks to add the movie`() =
        assertToolbar("detail_toolbar_signed_out.json", "favorite_border", nextState = true)

    @Test
    fun `favorited, the filled heart asks to remove it`() =
        assertToolbar("detail_toolbar_favorited.json", "favorite", nextState = false)
}
