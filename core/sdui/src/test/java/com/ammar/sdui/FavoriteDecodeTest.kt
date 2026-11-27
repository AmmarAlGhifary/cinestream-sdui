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

/** The detail toolbar of the favorites contract: the heart's body carries a real JSON boolean. */
class FavoriteDecodeTest {

    private val errors = mutableListOf<DecodeError>()
    private val blueprint = Blueprint(CineStreamRegistry, onDecodeError = { errors += it })

    private val toolbar = """
        {"type":"app_bar",
         "title":{"type":"toolbar_title","text_content":"CineStream","alignment":"start","style":"headline"},
         "actions":[
           {"type":"icon_button","icon_name":"favorite_border",
            "action":{"type":"api_action","endpoint":"/api/favorite","method":"POST",
                      "body":{"movie_id":"550","favorite":true}}},
           {"type":"icon_button","icon_name":"share",
            "action":{"type":"share_action","content":"Check out Fight Club on CineStream!"}}
         ]}
    """.trimIndent()

    @Test
    fun `the heart stays an api action and its body keeps the JSON types`() {
        val appBar = blueprint.decodeScreen("""{"screen_id":"movie_detail_screen","body":$toolbar}""").body
            as SduiAppBar

        val (heart, share) = appBar.actions.map { it as SduiIconButton }
        assertEquals(emptyList<DecodeError>(), errors)
        assertEquals("favorite_border", heart.iconName)
        val action = heart.action as ApiAction
        assertEquals("/api/favorite", action.endpoint)
        assertEquals("POST", action.method)
        val body = action.body!!
        assertEquals(JsonPrimitive("550"), body["movie_id"])
        val favorite = body["favorite"] as JsonPrimitive
        assertFalse("a boolean, not a string", favorite.isString)
        assertTrue(favorite.boolean)
        assertEquals("""{"movie_id":"550","favorite":true}""", body.toString())
        assertTrue(share.action is ShareAction)
    }
}
