package com.ammar.sdui

import com.ammar.network.error.AppError
import com.ammar.network.source.ActionDataSource
import com.ammar.network.source.NetworkDataSource
import com.ammar.sdui.data.repository.ActionRepositoryImpl
import com.ammar.sdui.data.repository.SduiRepositoryImpl
import com.ammar.sdui.domain.model.ActionResult
import com.ammar.sdui.domain.model.ApiAction
import com.ammar.sdui.domain.model.SduiAppBar
import com.ammar.sdui.domain.model.SduiIconButton
import com.ammar.sdui.domain.model.SduiScaffold
import com.ammar.sdui.domain.model.updatedBy
import com.ammar.sdui.presentation.registry.CineStreamRegistry
import io.github.ammaralghifary.blueprint.Blueprint
import io.github.ammaralghifary.blueprint.model.SduiScreen
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

/** What happens to the heart after the favorite request is answered. */
class FavoriteUpdateTest {

    private val blueprint = Blueprint(CineStreamRegistry)

    private fun heart(icon: String, next: Boolean, movie: String = "42") =
        """{"type":"icon_button","icon_name":"$icon","action":{"type":"api_action",
           "endpoint":"/api/favorite","method":"POST","body":{"movie_id":"$movie","favorite":$next}}}"""

    private fun fixture(name: String) = requireNotNull(javaClass.classLoader?.getResource(name)).readText()

    private fun detail(toolbar: String): SduiScreen {
        val json = """{"screen_id":"movie_detail_screen","toolbar":${fixture(toolbar)},
            "body":{"type":"column","children":[]}}"""
        val source = object : NetworkDataSource {
            override fun getScreenBlueprint(screenId: String, queryParams: Map<String, String>): Flow<Result<String>> =
                flowOf(Result.success(json))
        }
        return runBlocking {
            SduiRepositoryImpl(source, blueprint).getScreen("movie_detail_screen").first().getOrThrow()
        }
    }

    private val screen: SduiScreen = detail("detail_toolbar_signed_out.json")

    private fun SduiScreen.buttons() =
        ((body as SduiScaffold).toolbar as SduiAppBar).actions.map { it as SduiIconButton }

    private val tapped = screen.buttons()[0].action as ApiAction

    private fun answer(response: String): ActionResult = runBlocking {
        val source = object : ActionDataSource {
            override suspend fun send(method: String, endpoint: String, body: JsonObject?) = Result.success(response)
        }
        ActionRepositoryImpl(source, blueprint).send(tapped).getOrThrow()
    }

    private fun assertHeart(updated: SduiScreen, icon: String, next: Boolean) {
        val (heart, share) = updated.buttons()
        assertEquals(icon, heart.iconName)
        assertEquals(
            """{"movie_id":"42","favorite":$next}""",
            (heart.action as ApiAction).body.toString()
        )
        assertSame(screen.buttons()[1], share)
    }

    @Test
    fun `the returned button replaces the heart, giving the favorited toolbar`() {
        val result = answer(fixture("favorite_response.json"))

        assertEquals(true, result.isFavorite)
        val updated = screen.updatedBy(tapped, result)!!
        assertHeart(updated, "favorite", next = false)
        assertEquals(detail("detail_toolbar_favorited.json"), updated)
    }

    @Test
    fun `without a usable button the favorite state redraws the heart`() {
        val answers = listOf(
            """{"is_favorite":true}""",
            """{"is_favorite":true,"icon_button":{"type":"hologram"}}""",
            """{"is_favorite":true,"icon_button":{"type":"icon_button","icon_name":"favorite"}}""",
            """{"is_favorite":true,"icon_button":"favorite"}"""
        )
        for (body in answers) {
            val result = answer(body)
            assertNull(body, result.iconButton)
            assertHeart(screen.updatedBy(tapped, result)!!, "favorite", next = false)
        }
    }

    @Test
    fun `an answer with nothing usable cannot update the screen`() {
        for (body in listOf("", "{", "[]", "{}", """{"is_favorite":"true"}""")) {
            assertNull(body, screen.updatedBy(tapped, answer(body)))
        }
    }

    @Test
    fun `an action without a button on this screen cannot update it`() {
        val otherMovie = (blueprint.decodeScreen("""{"screen_id":"x","body":${heart("favorite", false, "9")}}""")
            .body as SduiIconButton).action as ApiAction

        assertNull(screen.updatedBy(otherMovie, ActionResult(isFavorite = true)))
    }

    @Test
    fun `a failed request stays a failure`() = runBlocking {
        val failure = AppError.NoConnection()
        val source = object : ActionDataSource {
            override suspend fun send(method: String, endpoint: String, body: JsonObject?) =
                Result.failure<String>(failure)
        }

        assertSame(failure, ActionRepositoryImpl(source, blueprint).send(tapped).exceptionOrNull())
    }
}
