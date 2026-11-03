package com.ammar.sdui

import com.ammar.network.source.NetworkDataSource
import com.ammar.sdui.data.repository.SduiRepositoryImpl
import com.ammar.sdui.domain.model.SduiAppBar
import com.ammar.sdui.domain.model.SduiColumn
import com.ammar.sdui.domain.model.SduiScaffold
import com.ammar.sdui.presentation.content
import com.ammar.sdui.presentation.registry.CineStreamRegistry
import io.github.ammaralghifary.blueprint.Blueprint
import io.github.ammaralghifary.blueprint.model.DecodeError
import io.github.ammaralghifary.blueprint.model.SduiScreen
import io.github.ammaralghifary.blueprint.model.UnknownComponent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Decodes real sdui-backend payloads (captured into test resources) through the Blueprint registry. */
class BlueprintDecodeTest {

    private val errors = mutableListOf<DecodeError>()
    private val blueprint = Blueprint(CineStreamRegistry, onDecodeError = { errors += it })

    private fun decode(json: String): Result<SduiScreen> = runBlocking {
        val source = object : NetworkDataSource {
            override fun getScreenBlueprint(screenId: String, queryParams: Map<String, String>): Flow<Result<String>> =
                flowOf(Result.success(json))
        }
        SduiRepositoryImpl(source, blueprint).getScreen("any").first()
    }

    private fun fixture(name: String): String =
        requireNotNull(javaClass.classLoader?.getResource(name)) { "missing $name" }.readText()

    @Test
    fun `every backend screen decodes without a single tolerated problem`() {
        for (name in listOf("home.json", "detail.json", "list.json", "search.json")) {
            errors.clear()
            val screen = decode(fixture(name)).getOrThrow()
            assertTrue("$name: body should be the scaffold", screen.body is SduiScaffold)
            assertEquals("$name: ${errors.map { "${it.kind} ${it.type}: ${it.message}" }}", emptyList<DecodeError>(), errors)
        }
    }

    @Test
    fun `toolbar is folded into the scaffold and content skips it`() {
        val screen = decode(fixture("home.json")).getOrThrow()
        val scaffold = screen.body as SduiScaffold
        assertEquals("home", screen.id)
        assertTrue(scaffold.toolbar is SduiAppBar)
        assertTrue(screen.content is SduiColumn)
    }

    @Test
    fun `an unknown node degrades alone and its siblings survive`() {
        val json = """
            {"type":"screen","screen_id":"x","body":{"type":"column","children":[
              {"type":"text","text_content":"kept"},
              {"type":"hologram","beam":true},
              {"type":"movie_card","poster_url":"p"}
            ]}}
        """.trimIndent()
        val column = decode(json).getOrThrow().content as SduiColumn

        assertEquals(3, column.children.size)
        assertTrue(column.children[1] is UnknownComponent)   // unregistered type
        assertTrue(column.children[2] is UnknownComponent)   // registered, but movie_id/title missing
        assertEquals(2, errors.size)
    }

    @Test
    fun `an unreadable payload is still a failure`() {
        val result = decode("<html>502 Bad Gateway</html>")
        assertTrue(result.isFailure)
        assertNull(result.getOrNull())
    }
}
